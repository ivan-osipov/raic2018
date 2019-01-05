import kotlin.math.absoluteValue

class AttackBehaviour(strategy: MyStrategy) : AbstractBehaviour<AttackBehaviour.State>(strategy) {

    override fun perform() {
        var bestPositionToKick = simulator.findBestPlaceToKickOut(myEntity, game.current_tick, predictedWorldStates)
        bestPositionToKick = findBestPositionToKickInGoalsDirection(bestPositionToKick)
        val allOpponentsAreOnTheirSide = opponents.map { it.z }.all { it > 0 }
        val zPos = if (allOpponentsAreOnTheirSide) {
            if (bestPositionToKick.z < -rules.arena.depth / 5) {
                -rules.arena.depth / 5
            } else {
                bestPositionToKick.z
            }
        } else bestPositionToKick.z
        targetPositions[me.id] = Vector3d(bestPositionToKick.x, myEntity.radius, zPos)

        when (getCurrentState()) {
            State.GO_TO -> {
                goToTargetPosition(strategy.ballIsPlayed)
            }
            State.KICK -> kickBall()
        }
    }

    override fun getCurrentState(): State {
        return if ((strategy.me.x - strategy.game.ball.x).absoluteValue < (strategy.game.ball.radius + strategy.me.radius) * 1.1
                && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25
                && strategy.me.z < strategy.game.ball.z
                && strategy.game.ball.y > (strategy.game.ball.radius + strategy.me.radius) * 1.1) State.KICK else State.GO_TO
    }

    private fun findBestPositionToKickInGoalsDirection(predictedBallPosition: Vector3d): Vector3d {
        return predictedBallPosition + (predictedBallPosition - frontOpponentGoalPoint).normalize() * (ballEntity.radius + myEntity.radius)
    }

    enum class State {
        GO_TO,
        KICK
    }
}
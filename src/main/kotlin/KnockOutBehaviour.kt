import kotlin.math.absoluteValue

class KnockOutBehaviour(strategy: MyStrategy) : AbstractBehaviour<KnockOutBehaviour.State>(strategy) {

    override fun perform() {
        targetPositions[me.id] = simulator.findBestPlaceToKickOut(myEntity, game.current_tick, predictedWorldStates)

        when (getCurrentState()) {
            State.GO_TO -> {
                goToTargetPosition()
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

    enum class State {
        GO_TO,
        KICK
    }
}
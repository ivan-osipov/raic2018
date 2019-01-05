class DefenceBehaviour(strategy: MyStrategy) : AbstractBehaviour<DefenceBehaviour.State>(strategy) {

    override fun perform() {
        val predictedBallPositionIndexBeforeMyGoal = simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)

        if (predictedBallPositionIndexBeforeMyGoal != null) {
            targetPositions[me.id] = simulator.findBestPlaceToKickOut(myEntity, game.current_tick, predictedWorldStates)
        } else {
            targetPositions[me.id] = Vector3d(clamp(ballEntity.position.x, minGoalX, maxGoalX), myEntity.radius, goalZ)
        }

        when (getCurrentState()) {
            State.HOLD -> {
                goToTargetPosition()
            }
        }
    }

    override fun getCurrentState(): State {
        return State.HOLD
    }

    enum class State {
        HOLD
    }
}
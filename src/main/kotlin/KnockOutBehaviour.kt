class KnockOutBehaviour(strategy: MyStrategy) : AbstractBehaviour(strategy) {

    override fun perform() {
        if (game.ball.velocity_z < 0) {
            val knockOutStateIndex = simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)
            val targetPositionIndex = knockOutStateIndex ?: clamp(10, 0, predictedBallPositions.size - 1)
            val worldStateOnKnockingOut = predictedWorldStates[targetPositionIndex]
            targetPositions[me.id] = worldStateOnKnockingOut.ball.position
        } else {
            targetPositions[me.id] = ballEntity.position
        }
        goToTargetPosition()
        kickIfPossible()
    }
}
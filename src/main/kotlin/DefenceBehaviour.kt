import kotlin.math.ceil

class DefenceBehaviour(strategy: MyStrategy) : AbstractBehaviour(strategy) {

    override fun perform() {
        val predictedBallPositionIndexBeforeMyGoal = simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)

        if (predictedBallPositionIndexBeforeMyGoal != null) {
            val coef = distance(myEntity, ballEntity) / distance(myEntity, predictedWorldStates[predictedBallPositionIndexBeforeMyGoal].ball)
            val (x, y, z) = predictedBallPositions[clamp(ceil(predictedBallPositionIndexBeforeMyGoal * coef).toInt(), 0, predictedBallPositionIndexBeforeMyGoal)]
            val targetPosition = Vector3d(x, y, z)
            targetPositions[me.id] = targetPosition
            if (distance(ballEntity.position, frontGoalPoint) <= distance(myEntity.position, frontGoalPoint)
                    && (ballEntity.velocity.y > 0 || ballEntity.position.y > (ballEntity.radius + myEntity.radius))) {
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED
            }
        } else {
            targetPositions[me.id] = Vector3d(clamp(ballEntity.position.x, minGoalX, maxGoalX), myEntity.radius, goalZ)
        }

        goToTargetPosition()
        kickIfPossible()
    }
}
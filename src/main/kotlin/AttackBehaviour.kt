import kotlin.math.ceil

class AttackBehaviour(strategy: MyStrategy) : AbstractBehaviour(strategy) {

    override fun perform() {
        val forwardBallMeetingTime = simulator.timeToMeetingManagedAndUnmanagedObjects(myEntity, ballEntity)
        val index = ceil(forwardBallMeetingTime / simulator.deltaTime()).toInt()
        val predictedBallPosition = predictedBallPositions[clamp(index, 0, predictedBallPositions.size - 1)]
        val bestPositionToKick = findBestPositionToKick(if (predictedBallPosition.y > me.radius * 2) ballEntity.position else predictedBallPosition)
        val allOpponentsAreOnTheirSide = opponents.map { it.z }.all { it > 0 }
        strategy.forwardsAreWaiting = false
        val zPos = if (allOpponentsAreOnTheirSide) {
            if (bestPositionToKick.z < -rules.arena.depth / 5) {
                strategy.forwardsAreWaiting = true
                -rules.arena.depth / 5
            } else {
                bestPositionToKick.z
            }
        } else bestPositionToKick.z
        targetPositions[me.id] = Vector3d(bestPositionToKick.x, 0.0, zPos)

        goToTargetPosition()
        kickIfPossible()
    }
}
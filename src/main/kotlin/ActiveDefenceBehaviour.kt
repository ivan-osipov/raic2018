class ActiveDefenceBehaviour(strategy: MyStrategy) : AbstractBehaviour(strategy) {

    override fun perform() {
        val targetOpponent = opponents
                .sortedBy { distance(entitiesByRobotIds[it.id]!!.position, ballEntity.position) }
                .first()

        val targetOpponentEntity = entitiesByRobotIds[targetOpponent.id]!!
        if (distance(myEntity.position, targetOpponentEntity.position) > myEntity.radius * 1.1 + targetOpponentEntity.radius) {
            targetPositions[myEntity.id] = targetOpponentEntity.position //TODO use future position
            goToTargetPosition()
        } else {
            kickIt(targetOpponentEntity.position)
        }
    }
}
class ActiveDefenceBehaviour(strategy: MyStrategy) : AbstractBehaviour<ActiveDefenceBehaviour.State>(strategy) {

    override fun perform() {
        val targetOpponent = opponents
                .sortedBy { distance(entitiesByRobotIds[it.id]!!.position, ballEntity.position) }
                .first()
        val targetOpponentEntity = entitiesByRobotIds[targetOpponent.id]!!

        when (getCurrentState()) {
            State.GO_TO -> {
                targetPositions[myEntity.id] = simulator.findBestPlaceTo(myEntity, targetOpponentEntity, game.current_tick, predictedWorldStates)
                goToTargetPosition()
            }
            State.KICK -> kickIt(targetOpponentEntity.position)
        }
    }

    override fun getCurrentState(): State {
        val targetOpponent = opponents
                .sortedBy { distance(entitiesByRobotIds[it.id]!!.position, ballEntity.position) }
                .first()
        val targetOpponentEntity = entitiesByRobotIds[targetOpponent.id]!!

        return if (distance(myEntity, targetOpponentEntity) > (myEntity.radius + targetOpponentEntity.radius) * 1.1) State.GO_TO else State.KICK
    }

    enum class State {
        GO_TO,
        KICK
    }
}
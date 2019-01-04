class StateDefinitionHelper(
        strategy: MyStrategy
) : StrategyComponent(strategy) {

    fun computeStateMap(): Map<Int, RobotState> {
        if (strategy.states.isEmpty()) {
            val (teammate1, teammate2) = strategy.teammates
                    .sortedBy { distance(strategy.entitiesByRobotIds[it.id]!!.position, strategy.frontGoalPoint) }
            return mapOf(
                    teammate1.id to RobotState.DEFENCE,
                    teammate2.id to RobotState.ATTACK
            )
        }
        return computeStateMapOnTopOfExperience()
    }

    private fun computeStateMapOnTopOfExperience(): Map<Int, RobotState> {
        val previousStates = HashMap(strategy.states)
        val newStatesMap = HashMap<Int, RobotState>()
        for ((teammateId, state) in previousStates) {
            newStatesMap[teammateId] = when (state!!) {
                RobotState.ATTACK -> defineStateAfterAttack(previousStates, teammateId)
                RobotState.ACTIVE_DEFENCE -> defineStateAfterActiveDefence(previousStates, teammateId)
                RobotState.KNOCKING_OUT -> defineStateAfterKnockingOut(previousStates, teammateId)
                RobotState.DEFENCE -> defineStateAfterDefence(previousStates, teammateId)
            }
        }
        return newStatesMap
    }

    private fun defineStateAfterAttack(previousStates: Map<Int, RobotState>, teammateId: Int): RobotState {
        if(readyToAttack(previousStates, teammateId)) {
            return RobotState.ATTACK
        }
        if(readyToActiveDefence(previousStates, teammateId)) {
            return RobotState.ACTIVE_DEFENCE
        }
        if (readyToKnockOut(previousStates, teammateId)) {
            return RobotState.KNOCKING_OUT
        }
        if(readyToDefence(previousStates, teammateId)) {
            return RobotState.DEFENCE
        }
        return RobotState.ATTACK
    }

    private fun defineStateAfterActiveDefence(previousStates: Map<Int, RobotState>, teammateId: Int): RobotState {
        if(readyToActiveDefence(previousStates, teammateId)) {
            return RobotState.ACTIVE_DEFENCE
        }
        if(readyToAttack(previousStates, teammateId)) {
            return RobotState.ATTACK
        }
        if (readyToKnockOut(previousStates, teammateId)) {
            return RobotState.KNOCKING_OUT
        }
        if(readyToDefence(previousStates, teammateId)) {
            return RobotState.DEFENCE
        }
        return RobotState.ACTIVE_DEFENCE
    }

    private fun defineStateAfterKnockingOut(previousStates: Map<Int, RobotState>, teammateId: Int): RobotState {
        if (readyToKnockOut(previousStates, teammateId)) {
            return RobotState.KNOCKING_OUT
        }
        if(readyToDefence(previousStates, teammateId)) {
            return RobotState.DEFENCE
        }
        if(readyToActiveDefence(previousStates, teammateId)) {
            return RobotState.ACTIVE_DEFENCE
        }
        if(readyToAttack(previousStates, teammateId)) {
            return RobotState.ATTACK
        }
        return RobotState.ATTACK
    }

    private fun defineStateAfterDefence(previousStates: Map<Int, RobotState>, teammateId: Int): RobotState {
        if (readyToKnockOut(previousStates, teammateId)) {
            return RobotState.KNOCKING_OUT
        }
        if(readyToDefence(previousStates, teammateId)) {
            return RobotState.DEFENCE
        }
        if(readyToActiveDefence(previousStates, teammateId)) {
            return RobotState.ACTIVE_DEFENCE
        }
        if(readyToAttack(previousStates, teammateId)) {
            return RobotState.ATTACK
        }
        return RobotState.DEFENCE
    }

    private fun readyToActiveDefence(previousStates: Map<Int, RobotState>, teammateId: Int): Boolean {
        return isNobodyInState(teammateId, previousStates, RobotState.ACTIVE_DEFENCE)
                && isBallOnOurSide()
                && isOpponentOnOurSide()
    }

    private fun readyToAttack(previousStates: Map<Int, RobotState>, teammateId: Int): Boolean {
        return isNobodyInState(teammateId, previousStates, RobotState.ATTACK)
                && !isNobodyInState(teammateId, previousStates, RobotState.DEFENCE)
                && isRobotNearestToBall(teammateId)
    }

    private fun readyToDefence(previousStates: Map<Int, RobotState>, teammateId: Int): Boolean {
        return isNobodyInState(teammateId, previousStates, RobotState.DEFENCE)
            && isRobotNearestToOurGoal(teammateId)
    }

    private fun readyToKnockOut(previousStates: Map<Int, RobotState>, teammateId: Int): Boolean {
        val robotEntity = entitiesByRobotIds[teammateId]!!
        val nobodyIsOnKnockingOut = isNobodyInState(teammateId, previousStates, RobotState.KNOCKING_OUT)
        val currentPositionIsNearerToOurGoals = distance(ballEntity.position, frontGoalPoint) - distance(robotEntity.position, frontGoalPoint) > ballEntity.radius
        val distanceToBall = distance(robotEntity, ballEntity)
        val nearerThenOther = game.robots.asSequence()
                .filter { it -> it.id != teammateId }
                .all { distance(entitiesByRobotIds[it.id]!!, ballEntity) > distanceToBall }

        return nobodyIsOnKnockingOut
                && isBallOnOurSide()
                && currentPositionIsNearerToOurGoals
                && nearerThenOther
    }

    private fun isRobotNearestToOurGoal(teammateId: Int): Boolean {
        return teammates
                .sortedBy { distance(frontGoalPoint, entitiesByRobotIds[it.id]!!.position) }
                .first().id == teammateId
    }

    private fun isRobotNearestToBall(teammateId: Int): Boolean {
        return teammates
                .sortedBy { distance(ballEntity.position, entitiesByRobotIds[it.id]!!.position) }
                .first().id == teammateId
    }

    private fun isBallOnOurSide() = game.ball.z < 0

    private fun isBallNearToGoals() = game.ball.z < -rules.arena.depth / 4

    private fun isOpponentOnOurSide() = opponents.any { it.z < 0 }

    private fun isNobodyInState(teammateId: Int, previousStates: Map<Int, RobotState>, state: RobotState): Boolean {
        return strategy.teammates.asSequence()
                .filter { it.id != teammateId }
                .map { previousStates[it.id] }
                .none { it == state }
    }

}
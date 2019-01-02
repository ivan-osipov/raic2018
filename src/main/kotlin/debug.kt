fun collectDebugInfo(strategy: MyStrategy): List<IDebugInfo> {
    val teammateDebugInfo = strategy.teammates
            .flatMap {
                val (r, g, b) = when (strategy.states[it.id]) {
                    RobotState.DEFENCE -> Vector3d(0.0, 1.0, 0.0)
                    RobotState.KNOCKING_OUT -> Vector3d(1.0, 1.0, 0.0)
                    RobotState.ATTACK -> Vector3d(1.0, 0.0, 0.0)
                    RobotState.WAITING -> Vector3d(0.4, 0.4, 0.4)
                    RobotState.RUNNING_ON_POSITION -> Vector3d(0.0, 0.0, 1.0)
                    else -> throw IllegalStateException("Unsupported state")
                }
                val targetPosition = strategy.targetPositions[it.id]!!
                val teammateDebugInfo = mutableListOf(
                        Text("${it.id} ${strategy.states[it.id]} {x:${"%.2f".format(it.x)} y:${"%.2f".format(it.y)} z:${"%.2f".format(it.z)}} " +
                                "{x:${"%.2f".format(targetPosition.x)} y:${"%.2f".format(targetPosition.y)} z:${"%.2f".format(targetPosition.z)}}"),
                        LineContainer(Line(it.x, it.y, it.z,
                                it.x + it.velocity_x, it.y + it.velocity_y, it.z + it.velocity_z)),
                        SphereContainer(Sphere(it.x + it.radius, it.y + it.radius, it.z + it.radius, 0.1, r, g, b)))
                strategy.targetPositions[it.id]?.let { target ->
                    teammateDebugInfo.add(SphereContainer(Sphere(target.x, target.y, target.z,
                            0.5, 0.0, 0.0, 1.0))) //blue
                }
                teammateDebugInfo
            }

    val opponentsDebugInfo = strategy.opponents
            .flatMap { opponent ->
                val opponentEntity = strategy.entitiesByRobotIds[opponent.id]!!
                val prediction = strategy.predictedRobotState[opponent.id]!!
                val predictedState = prediction.last()
                val predictedPosition = predictedState.position
//                val predictedKick = predictedCollision(opponentEntity, strategy.ballEntity, strategy.simulator)
                val setOfDebug = mutableSetOf(
                        LineContainer(Line(opponentEntity.position, predictedPosition, 0.5, 1.0, 0.0, 0.0)),
                        SphereContainer(Sphere(predictedPosition, 0.5, 1.0, 0.0, 0.0))) //red
                var i = 0
                prediction.windowed(2) {
                    val (first, second) = it
                    setOfDebug.add(LineContainer(Line(first.position, second.position, 0.5, 1.0, 1.0, 0.0, (prediction.size - i++) / 10.0)))
                }
//                predictedKick?.let {
//                    setOfDebug.add(SphereContainer(Sphere(it.first, 0.75, 1.0, 1.0, 0.0))) //yellow
//                    setOfDebug.add(Text("Robot: ${opponent.id} are going to kick from $it after ${it.second}"))
//                }
                setOfDebug
            }//TODO выбивать в направлении чужих ворот

    val commonDebugInfo = mutableListOf<IDebugInfo>(Text(strategy.game.current_tick.toString()))

    for ((point, value) in strategy.potentialFields.getScores()) {
        val (z, x) = point
        commonDebugInfo.add(SphereContainer(Sphere(Vector3d(x, 0.0, z), 0.3, value, 0.0, 0.0)))
    }

    val predictedBallPositions = strategy.predictedBallPositions
    val predictedBallPositionIndexBeforeMyGoal = strategy.simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)
    var i = 0
    predictedBallPositions.windowed(2) {
        val (first, second) = it
        val (r, g, b) = if (predictedBallPositionIndexBeforeMyGoal != null && predictedBallPositionIndexBeforeMyGoal < i) {
            Vector3d(1.0, 0.0, 0.0)
        }
        else Vector3d(1.0, 1.0, 1.0)
        commonDebugInfo.add(LineContainer(Line(first, second, 0.5, r, g, b, (predictedBallPositions.size - i++) / 10.0)))
    }

    return teammateDebugInfo + commonDebugInfo + opponentsDebugInfo
}
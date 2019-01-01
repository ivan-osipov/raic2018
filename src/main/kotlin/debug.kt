import model.Game
import model.Robot

fun collectDebugInfo(robotStates: Map<Int, RobotState>,
                     teammates: Collection<Robot>,
                     opponents: Collection<Robot>,
                     game: Game,
                     targetPosition: Map<Int, Vector3d>,
                     potentialFields: PotentialFields,
                     simulator: Simulator): List<IDebugInfo> {
    val teammateDebugInfo = teammates
            .flatMap {
                val (r, g, b) = when (robotStates[it.id]) {
                    RobotState.DEFENCE -> Vector3d(0.0, 1.0, 0.0)
                    RobotState.KNOCKING_OUT -> Vector3d(1.0, 1.0, 0.0)
                    RobotState.ATTACK -> Vector3d(1.0, 0.0, 0.0)
                    RobotState.WAITING -> Vector3d(0.4, 0.4, 0.4)
                    RobotState.RUNNING_ON_POSITION -> Vector3d(0.0, 0.0, 1.0)
                    else -> throw IllegalStateException("Unsupported state")
                }
                val teammateDebugInfo = mutableListOf(
                        Text("${it.id} ${robotStates[it.id]} {x:${"%.2f".format(it.x)} y:${"%.2f".format(it.y)} z:${"%.2f".format(it.z)}}"),
                        LineContainer(Line(it.x, it.y, it.z,
                                it.x + it.velocity_x, it.y + it.velocity_y, it.z + it.velocity_z)),
                        SphereContainer(Sphere(it.x + it.radius, it.y + it.radius, it.z + it.radius, 0.1, r, g, b)))
                targetPosition[it.id]?.let { target ->
                    teammateDebugInfo.add(SphereContainer(Sphere(target.x, target.y, target.z,
                            0.5, 0.0, 0.0, 1.0))) //blue
                }
                teammateDebugInfo
            }

    val ballEntity = game.ball.toEntity(simulator.rules)
    val opponentsDebugInfo = opponents
            .flatMap { opponent ->
                val predictedPosition = opponent.toActiveObject(simulator.rules).predictedPosition(1.0, simulator)
                val predictedKick = predictedCollision(opponent.toActiveObject(simulator.rules), ballEntity, simulator)
                val setOfDebug = mutableSetOf(
                        LineContainer(Line(opponent.toActiveObject(simulator.rules).position, predictedPosition, 0.5, 1.0, 0.0, 0.0)),
                        SphereContainer(Sphere(predictedPosition, 0.5, 1.0, 0.0, 0.0))) //red
                predictedKick?.let {
                    setOfDebug.add(SphereContainer(Sphere(it.first, 0.75, 1.0, 1.0, 0.0))) //yellow
                    setOfDebug.add(Text("Robot: ${opponent.id} are going to kick from $it after ${it.second}"))
                }
                setOfDebug
            }

    val commonDebugInfo = mutableListOf<IDebugInfo>(Text(game.current_tick.toString()))

    for ((point, value) in potentialFields.getScores()) {
        val (z, x) = point
        commonDebugInfo.add(SphereContainer(Sphere(Vector3d(x, 0.0, z), 0.3, value, 0.0, 0.0)))
    }

    val predictedBallPositions = ballEntity.predictedPositionChain(1.0, simulator)
    var i = 0
    for (predictedBallPosition in predictedBallPositions) {
        commonDebugInfo.add(SphereContainer(Sphere(predictedBallPosition, 0.5, 1.0, 1.0, 1.0, (predictedBallPositions.size - i++) / 10.0)))
    }

    return teammateDebugInfo + commonDebugInfo + opponentsDebugInfo
}
import model.Game
import model.Robot
import model.Rules

fun collectDebugInfo(teammates: Collection<Robot>,
                     opponents: Collection<Robot>,
                     game: Game,
                     rules: Rules,
                     targetPosition: Map<Int, Point>,
                     potentialFields: PotentialFields): List<IDebugInfo> {
    val goalkeeper = teammates.goalkeeper
    val teammateDebugInfo = teammates
            .flatMap {
                val r = if (it.id == goalkeeper.id) 0.0 else 1.0
                val g = if (it.id == goalkeeper.id) 1.0 else 0.0
                val b = 0.0
                val teammateDebugInfo = mutableListOf(
                        Text("${it.id} (gk: ${it.id == goalkeeper.id}): {x:${"%.2f".format(it.x)} y:${"%.2f".format(it.y)} z:${"%.2f".format(it.z)}}"),
                        LineContainer(Line(it.x, it.y, it.z,
                                it.x + it.velocity_x, it.y + it.velocity_y, it.z + it.velocity_z)),
                        SphereContainer(Sphere(it.x + it.radius, it.y + it.radius, it.z + it.radius,
                                0.1, r, g, b))) //marker green - goalkeeper, red - forward
                targetPosition[it.id]?.let { target ->
                    teammateDebugInfo.add(SphereContainer(Sphere(target.x, target.y, target.z,
                            0.5, 0.0, 1.0, 0.0))) //blue
                }
                teammateDebugInfo
            }

    val opponentsDebugInfo = opponents
            .flatMap { opponent ->
                val predictedPosition = opponent.toActiveObject().predictedPosition(1.0, rules.arena)
                val predictedKick = predictedCollision(opponent.toActiveObject(), game.ball.toActiveObject())
                val setOfDebug = mutableSetOf(
                        LineContainer(Line(opponent.toActiveObject().position, predictedPosition, 0.5, 1.0, 0.0, 0.0)),
                        SphereContainer(Sphere(predictedPosition, 0.5, 1.0, 0.0, 0.0))) //red
                predictedKick?.let {
                    setOfDebug.add(SphereContainer(Sphere(it.first, 0.75, 1.0, 1.0, 0.0))) //yellow
                    setOfDebug.add(Text("Robot: ${opponent.id} are going to kick from $it after ${it.second}"))
                }
                setOfDebug
            }

    val timeToMeetingGkAndBall = timeToMeetingManagedAndUnmanagedObjects(goalkeeper.toActiveObject(), game.ball.toActiveObject())
    val predictedBallPosition = game.ball.toActiveObject().predictedPosition(timeToMeetingGkAndBall, rules.arena)
    val commonDebugInfo = mutableListOf(
            Text(game.current_tick.toString()),
            LineContainer(Line(game.ball.toActiveObject().position, predictedBallPosition, 0.5, 0.0, 0.0, 1.0)),
            SphereContainer(Sphere(predictedBallPosition, 0.5, 0.0, 0.0, 1.0)))

    for ((point, value) in potentialFields.getScores()) {
        val (z, x) = point
        commonDebugInfo.add(SphereContainer(Sphere(Point(x, 0.0, z), 0.3, value, 0.0, 0.0)))
    }

    return teammateDebugInfo + commonDebugInfo + opponentsDebugInfo
}
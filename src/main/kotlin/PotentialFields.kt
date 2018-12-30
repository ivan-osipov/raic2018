import model.Action
import model.Game
import model.Robot
import model.Rules
import java.lang.Math.floor
import java.lang.Math.max
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign

class PotentialFields(val robot: Robot, game: Game, val rules: Rules, val action: Action) {

    val potentialMap: Matrix<Double>

    init {
        val maxWidth = rules.arena.width
        val maxDepth = rules.arena.depth + rules.arena.goal_depth * 2
        val normalizedWidth: Int = Math.floor(maxWidth / robot.radius).toInt()
        val normalizedDepth: Int = Math.floor(maxDepth / robot.radius).toInt()

        val goalLeftOutPoint = mapOnField(-rules.arena.depth / 2, -rules.arena.goal_width / 2)
        val goalRightOutPoint = mapOnField(-rules.arena.depth / 2, rules.arena.goal_width / 2)

        val opponentGoalLeftOutPoint = Pair(normalizedDepth - goalLeftOutPoint.first, normalizedWidth - goalLeftOutPoint.second)
//        val opponentGoalRightOutPoint = Pair(normalizedDepth - goalRightOutPoint.first, normalizedWidth - goalRightOutPoint.second)

        potentialMap = Matrix(normalizedDepth, normalizedWidth) { x, y ->
            if ((x >= opponentGoalLeftOutPoint.first || x < goalLeftOutPoint.first)
                    && (y >= goalRightOutPoint.second || y < goalLeftOutPoint.second)) {
                1.0
            } else {
                0.0
            }
        }
        putBallOnMap(game, normalizedDepth, normalizedWidth)

        for (opponent in game.robots.opponents) {
            val opponentOnField = mapOnField(opponent.z, opponent.x)
            potentialMap[opponentOnField.first, opponentOnField.second] = 1.0
        }

        for (teammate in game.robots.teammates) {
            if (teammate.id == robot.id) continue
            val teammateOnField = mapOnField(teammate.z, teammate.x)
            potentialMap[teammateOnField.first, teammateOnField.second] = 1.0
        }

        if (false) {
            println("----------------------------")
            println("--------- ${potentialMap.width} x ${potentialMap.height} ---------")
            println("----------------------------")
            println(potentialMap)
        }
    }

    private fun putBallOnMap(game: Game, width: Int, height: Int) {
        val scale = 2
        val ballOnField = mapOnField(game.ball.z, game.ball.x)
        val scaledBallRadius = game.ball.radius * scale
        val steps = max(floor(scaledBallRadius / robot.radius).toInt(), 0)

        for (i in -steps..steps) {
            val x = ballOnField.first + i
            if (x < 0 || x >= width) continue

            for (j in -steps..steps) {
                val y = ballOnField.second + j
                if (y < 0 || y >= height) continue

                val inCircle = (x - ballOnField.first).toDouble().pow(2) +
                        (y - ballOnField.second).toDouble().pow(2) > steps.toDouble().pow(2)
                if(inCircle) {
                    continue
                }

                val distance = max(i.absoluteValue, j.absoluteValue)
                potentialMap[x, y] += if (distance < 2) 1.0 else 1.0 / distance
            }
        }

    }

    private fun mapOnField(z: Double, x: Double): Pair<Int, Int> {
        val movedX = z + rules.arena.depth / 2 + rules.arena.goal_depth
        val movedY = x + rules.arena.width / 2
        return Pair(
                Math.floor(movedX / robot.radius).toInt(),
                Math.floor(movedY / robot.radius).toInt()
        )
    }

    fun mapOnScreen(i: Int, j: Int): Pair<Double, Double> {
        return Pair(i * robot.radius - rules.arena.depth / 2 - rules.arena.goal_depth,
                j * robot.radius - rules.arena.width / 2)
    }

    fun getScores(): List<Pair<Pair<Double, Double>, Double>> {
        val scores = ArrayList<Pair<Pair<Double, Double>, Double>>()
        for (i in 0 until potentialMap.width) {
            for (j in 0 until potentialMap.height) {
                val score = potentialMap[i, j]
                if (score == 0.0) continue
                scores.add(mapOnScreen(i, j) to score)
            }
        }
        return scores
    }


    fun modifyWayPointsByCurrentTargetPosition(point: Point) {
        val (x, y) = mapOnField(point.z, point.x)
        val (robotX, robotY) = mapOnField(robot.z, robot.x)
        val diffX = robotX - x
        val diffY = robotY - y

        val nearestX = x + diffX.sign
        val nearestY = y + diffY.sign

        val nearestPointScore = potentialMap[nearestX, nearestY]
        if (nearestPointScore == 0.0) {
            return //do nothing
        }

        var targetNearestX = nearestX
        var targetNearestY = nearestY
        var bestScore = nearestPointScore
        var bestDistance = hypot(diffX.toDouble(), diffY.toDouble())
        var secondScore = bestScore
        var secondDistance = bestDistance
        var secondTargetNearestX = nearestX
        var secondTargetNearestY = nearestY
        var foundBetterByAllCriteria = false
        for (i in (nearestX - 1)..(nearestX + 1)) { //TODO find by only nearest to robot line
            for (j in (nearestY - 1)..(nearestY + 1)) {
                if (nearestPointScore <= 1.0) {
                    val score = potentialMap[i, j]
                    if (score < bestScore) {
                        val distance = hypot((i - robotX).toDouble(), (j - robotY).toDouble())
                        if (!foundBetterByAllCriteria || distance < bestDistance) {
                            targetNearestX = i
                            targetNearestY = j
                            bestScore = score
                            bestDistance = distance
                            foundBetterByAllCriteria = true
                        } else if (score < secondScore && distance < secondDistance) {
                            secondTargetNearestX = i
                            secondTargetNearestY = j
                            secondScore = score
                            secondDistance = distance
                        }
                    }
                }
            }
        }

        val bestX: Int
        val bestY: Int
        if (foundBetterByAllCriteria) {
            bestX = targetNearestX
            bestY = targetNearestY
        } else {
            bestX = secondTargetNearestX
            bestY = secondTargetNearestY
        }

        if (bestX == nearestX && bestY == nearestY) {
            return //found the same
        }



        val targetXDif = bestY - robotY
        val targetZDif = bestX - robotX
        val maxDiff = kotlin.math.max(targetXDif, targetZDif)
        val diffPerSpeedUnit = (maxDiff / rules.ROBOT_MAX_GROUND_SPEED).absoluteValue

        action.target_velocity_x = if (targetXDif.absoluteValue >= 2) targetXDif / diffPerSpeedUnit.safeZero() else (if (targetXDif.absoluteValue < 0.05) 0.0 else targetXDif.sign * rules.ROBOT_MAX_GROUND_SPEED / 4)
        action.target_velocity_z = if (targetZDif.absoluteValue >= 2) targetZDif / diffPerSpeedUnit.safeZero() else (if (targetZDif.absoluteValue < 0.05) 0.0 else targetZDif.sign * rules.ROBOT_MAX_GROUND_SPEED / 4)
    }
}

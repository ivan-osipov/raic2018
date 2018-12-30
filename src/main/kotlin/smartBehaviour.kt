import model.Arena
import model.Rules
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sign

val MAX_PREDICTED_TIME = 3.0

fun ActiveObject.predictedPosition(after: Double, arena: Arena): Point {
    val rawX = x + velocity_x * after
    val rawY = radius
    val rawZ = z + velocity_z * after

    var y = min(rawY, arena.height)
    var x = rawX.sign * min(rawX.absoluteValue, arena.width / 2)
    var z = rawZ.sign * min(rawZ.absoluteValue, arena.depth / 2 + arena.goal_depth)

    if (z.absoluteValue > arena.depth / 2) {
        x = x.sign * min(x.absoluteValue, arena.goal_width / 2)
        y = y.sign * min(y.absoluteValue, arena.goal_height / 2)
    }

    return Point(x, y, z)
}

fun ActiveObject.predictedPositionWithoutY(after: Double): Point {
    return Point(x + velocity_x * after, 0.0, z + velocity_z * after)
}

fun predictedCollision(obj1: ActiveObject, obj2: ActiveObject): Pair<Point, Double>? {
    val maxTime = MAX_PREDICTED_TIME //sec
    val step = 0.01
    val amount = (maxTime / step).toInt()
    for (i in 0..amount) {
        val time = step * i
        val obj1PredictedPos = obj1.predictedPositionWithoutY(time)
        val obj2PredictedPos = obj2.predictedPositionWithoutY(time)
        if (distance(obj1PredictedPos, obj2PredictedPos) <= obj1.radius + obj2.radius) {
            return Pair(obj2PredictedPos, time)
        }
    }
    return null
}

public fun timeToMeetingManagedAndUnmanagedObjects(managedObject: ActiveObject, unmanagedObject: ActiveObject): Double {
    return distance(managedObject, unmanagedObject) / unmanagedObject.velocity().safeZero()
}

public fun timeToMeetingRunningRobotAndUnmanagedObjects(runningRobot: ActiveObject, unmanagedObject: ActiveObject, rules: Rules): Double {
    return distance(runningRobot, unmanagedObject) / (rules.ROBOT_MAX_GROUND_SPEED / 2 + unmanagedObject.velocity().safeZero())
}
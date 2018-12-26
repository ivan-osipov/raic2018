fun ActiveObject.predictedPosition(after: Double): Point {
    return Point(x + velocity_x * after, y + velocity_y * after, z + velocity_z * after)
}

fun predictedCollision(obj1: ActiveObject, obj2: ActiveObject): Point? {
    val maxTime = 3.0 //sec
    val step = 0.01
    val amount = (maxTime / step).toInt()
    for (i in 0..amount) {
        val time = step * i
        val obj1PredictedPos = obj1.predictedPosition(time)
        val obj2PredictedPos = obj2.predictedPosition(time)
        if (distance(obj1PredictedPos, obj2PredictedPos) <= obj1.radius + obj2.radius) {
            return obj2PredictedPos
        }
    }
    return null
}

public fun timeToMeetingManagedAndUnmanagedObjects(managedObject: ActiveObject, unmanagedObject: ActiveObject): Double {
    return distance(managedObject, unmanagedObject) / unmanagedObject.velocity().safeZero()
}
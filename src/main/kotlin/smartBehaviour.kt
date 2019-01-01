import kotlin.math.ceil
import kotlin.math.min

val MAX_PREDICTED_TIME = 3.0

fun Entity.predictedPosition(afterSecs: Double, simulator: Simulator): Vector3d {
    val deltaTime = simulator.deltaTime(min(MAX_PREDICTED_TIME, afterSecs))
    val ticksCount = ceil(afterSecs / deltaTime).toInt()

    var predictedEntity = this
    for (tick in 0..ticksCount) {
        predictedEntity = simulator.move(predictedEntity, deltaTime)
    }

    return predictedEntity.position
}

fun Entity.predictedPositionChain(afterSecs: Double, simulator: Simulator): List<Vector3d> {
    val deltaTime = simulator.deltaTime(min(MAX_PREDICTED_TIME, afterSecs))
    val ticksCount = ceil(afterSecs / deltaTime).toInt()

    var predictedEntity = this
    val predictedPositions = ArrayList<Vector3d>()
    for (tick in 0..ticksCount) {
        predictedEntity = simulator.move(predictedEntity, deltaTime)
        simulator.collideWithArena(predictedEntity)?.let {
            predictedEntity = it
        }
        predictedPositions.add(predictedEntity.position)
    }

    return predictedPositions
}

fun predictedCollision(obj1: Entity, obj2: Entity, simulator: Simulator): Pair<Vector3d, Double>? {
    val maxTime = MAX_PREDICTED_TIME //sec
    val step = 0.01
    val amount = (maxTime / step).toInt()
    for (i in 0..amount) {
        val time = step * i
        val obj1PredictedPos = obj1.predictedPosition(time, simulator)
        val obj2PredictedPos = obj2.predictedPosition(time, simulator)
        if (distance(obj1PredictedPos, obj2PredictedPos) <= obj1.radius + obj2.radius) {
            return Pair(obj2PredictedPos, time)
        }
    }
    return null
}
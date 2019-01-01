import kotlin.math.pow
import kotlin.math.sqrt

data class Vector3d(
        var x: Double,
        var y: Double,
        var z: Double
) {

    companion object {
        private val ZERO_VECTOR = Vector3d(0.0, 0.0, 0.0)
    }

    operator fun minus(anotherVector: Vector3d) = Vector3d(x - anotherVector.x, y - anotherVector.y, z - anotherVector.z)

    operator fun plus(anotherVector: Vector3d) = Vector3d(x + anotherVector.x, y + anotherVector.y, z + anotherVector.z)

    operator fun times(anotherVector: Vector3d) = x * anotherVector.x + y * anotherVector.y + z * anotherVector.z

    operator fun times(scalar: Double) = Vector3d(x * scalar, y * scalar, z * scalar)

    operator fun div(divider: Double) = Vector3d(x / divider, y / divider, z / divider)

    fun length(): Double {
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    fun normalize(): Vector3d {
        val length = length()
        if(length > 0) {
            return this / length
        }
        return ZERO_VECTOR
    }
}
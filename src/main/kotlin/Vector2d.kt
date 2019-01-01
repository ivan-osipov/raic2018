import kotlin.math.pow
import kotlin.math.sqrt

data class Vector2d(
        var x: Double,
        var y: Double
) {

    companion object {
        private val ZERO_VECTOR = Vector2d(0.0, 0.0)
    }

    operator fun minus(anotherVector: Vector2d) = Vector2d(x - anotherVector.x, y - anotherVector.y)

    operator fun plus(anotherVector: Vector2d) = Vector2d(x + anotherVector.x, y + anotherVector.y)

    operator fun times(anotherVector: Vector2d) = x * anotherVector.x + y * anotherVector.y

    operator fun times(scalar: Double) = Vector2d(x * scalar, y * scalar)

    operator fun div(divider: Double) = Vector2d(x / divider, y / divider)


    fun length(): Double {
        return sqrt(x.pow(2) + y.pow(2))
    }

    fun normalize(): Vector2d {
        val length = length()
        if (length > 0) {
            return this / length
        }
        return ZERO_VECTOR
    }
}
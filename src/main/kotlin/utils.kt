import model.Ball
import model.Robot
import model.Rules
import kotlin.math.*

fun distance(from: Entity, to: Entity) = distance(from.position, to.position)

fun distance(from: Vector3d, to: Vector3d) = (to - from).length()

fun distanceOnFlat(e1: Entity, e2: Entity): Double {
    return sqrt(distance(e1, e2).pow(2) - (e1.radius - e2.radius).pow(2))
}

fun Double.safeZero() = if (this.absoluteValue < 0.000001) 0.000001 else this

fun Ball.toEntity(rules: Rules) = Entity(this, rules)

fun Robot.toEntity(rules: Rules) = Entity(this, rules)

val Array<Robot>.opponents
    get() = this.filter { !it.is_teammate }.sortedBy { it.id }

val Array<Robot>.teammates
    get() = this.filter { it.is_teammate }.sortedBy { it.id }

fun clamp(value: Double, minValue: Double, maxValue: Double) = max(min(value, maxValue), minValue)

fun clamp(value: Int, minValue: Int, maxValue: Int) = max(min(value, maxValue), minValue)
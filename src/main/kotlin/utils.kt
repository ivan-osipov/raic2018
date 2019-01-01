import model.Ball
import model.Robot
import model.Rules
import kotlin.math.absoluteValue

fun distance(from: Entity, to: Entity) = distance(from.position, to.position)

fun distance(from: Vector3d, to: Vector3d) = (to - from).length()

fun Double.sign(): Double = this.safeZero() / this.absoluteValue.safeZero()

fun Double.safeZero() = if (this.absoluteValue < 0.000001) 0.000001 else this

fun Ball.toEntity(rules: Rules) = Entity(this, rules)

fun Robot.toActiveObject(rules: Rules) = Entity(this, rules)

val Collection<Robot>.goalkeeper
    get() = this/*.sortedByDescending { distance(it, game.ball) }*/.first()

val Array<Robot>.opponents
    get() = this.filter { !it.is_teammate }.sortedBy { it.id }

val Array<Robot>.teammates
    get() = this.filter { it.is_teammate }.sortedBy { it.id }
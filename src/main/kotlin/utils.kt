import model.Ball
import model.Robot
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

fun distance(from: ActiveObject, to: ActiveObject) = distance(from.position, to.position)

fun distance(from: Point, to: Point) = sqrt((from.x - to.x).pow(2) + (from.y - to.y).pow(2) + (from.z - to.z).pow(2))

fun Double.sign(): Double = this.safeZero() / this.absoluteValue.safeZero()

fun Double.safeZero() = if (this.absoluteValue < 0.000001) 0.000001 else this

fun Ball.toActiveObject() = ActiveObject(this)

fun Robot.toActiveObject() = ActiveObject(this)

val Collection<Robot>.goalkeeper
    get() = this/*.sortedByDescending { distance(it, game.ball) }*/.first()

val Array<Robot>.opponents
    get() = this.filter { !it.is_teammate }.sortedBy { it.id }

val Array<Robot>.teammates
    get() = this.filter { it.is_teammate }.sortedBy { it.id }
import com.google.gson.GsonBuilder
import model.*
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class MyStrategy : Strategy {

    private val gson = GsonBuilder()
            .create()

    lateinit var me: Robot
    lateinit var rules: Rules
    lateinit var game: Game
    lateinit var action: Action

    val teammates
        get() = game.robots.filter { it.is_teammate }.sortedBy { it.id }

    val opponents
        get() = game.robots.filter { !it.is_teammate }.sortedBy { it.id }

    val List<Robot>.goalkeeper
        get() = this/*.sortedByDescending { distance(it, game.ball) }*/.first()

    var isGk: Boolean = false

    var targetX: Double = 0.0
    var targetZ: Double = 0.0

    val maxGoalX
        get() = rules.arena.goal_width / 2 - rules.arena.bottom_radius
    val minGoalX
        get() = -rules.arena.goal_width / 2 + rules.arena.bottom_radius

    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        init(me, rules, game, action)
        doBehaviour()
    }

    private fun doBehaviour() {
        manageGoalkeeper()
        manageForwards()
    }

    private fun manageGoalkeeper() {
        if (!isGk) return

        val goalZ = -rules.arena.depth / 2
        val ballMeetingTimeX = timeToMeetingBallWithGk()

        val predictedCollision = opponents.map { predictedCollision(it.toActiveObject(), game.ball.toActiveObject()) }.filterNotNull().firstOrNull()

        val predictedBallPosition = game.ball.toActiveObject().predictedPosition(ballMeetingTimeX)
        val rawTargetX = predictedCollision?.x ?: predictedBallPosition.x
        targetX = adjustGoalkeeperXPosition(rawTargetX)
        targetZ = goalZ

        val targetXDif = targetX - me.x
        val targetZDif = targetZ - me.z

        val distanceToBallX = predictedBallPosition.x - me.x
        val distanceToBallZ = predictedBallPosition.z - me.z

        val maxDiff = max(targetXDif, targetZDif)
        val diffPerSpeedUnit = (maxDiff / rules.ROBOT_MAX_GROUND_SPEED).absoluteValue

        action.target_velocity_x = if (targetXDif.absoluteValue >= 3) targetXDif / diffPerSpeedUnit.safeZero() else (if (targetXDif.absoluteValue < 0.01) 0.0 else targetXDif.sign() * rules.ROBOT_MAX_GROUND_SPEED / 4)
        action.target_velocity_z = if (targetZDif.absoluteValue >= 3) targetZDif / diffPerSpeedUnit.safeZero() else (if (targetZDif.absoluteValue < 0.01) 0.0 else targetXDif.sign() * rules.ROBOT_MAX_GROUND_SPEED / 4)

        if ((me.x - game.ball.x).absoluteValue < (game.ball.radius + me.radius) * 1.1 && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25) {
            if (me.z < game.ball.z) {
                action.jump_speed = computeJumpSpeed()
                action.target_velocity_x = distanceToBallX
                action.target_velocity_z = distanceToBallZ
            }
        }
    }

    private fun adjustGoalkeeperXPosition(rawTargetX: Double) = min(max(rawTargetX, minGoalX), maxGoalX)

    private fun timeToMeetingBallWithGk() = distance(ActiveObject(me), ActiveObject(game.ball)) / ActiveObject(game.ball).velocity().safeZero()

    private fun manageForwards() {
        if (isGk) return
        val targetPointX = game.ball.x - me.x //TODO учесть направление движения
        val targetPointZ = game.ball.z - me.z - game.ball.radius
        action.target_velocity_x = targetPointX.sign() * rules.ROBOT_MAX_GROUND_SPEED
        action.target_velocity_z = targetPointZ.sign() * rules.ROBOT_MAX_GROUND_SPEED
        if (targetPointX.absoluteValue <= me.radius && targetPointZ.absoluteValue <= me.radius) {
            if (me.z < game.ball.z) {
                action.jump_speed = computeJumpSpeed()
                action.target_velocity_x = 0.0
            }
        }
    }

    private fun computeJumpSpeed(): Double {
        if (game.ball.z < 0) {
            return rules.ROBOT_MAX_JUMP_SPEED
        } else {
            val halfOfDepth = rules.arena.goal_depth / 2
            val minJumpSpeed = rules.ROBOT_MAX_JUMP_SPEED * 0.1
            return max(rules.ROBOT_MAX_JUMP_SPEED * 0.8 * (halfOfDepth / (halfOfDepth + game.ball.z)), minJumpSpeed)
        }
    }

    private fun init(me: Robot, rules: Rules, game: Game, action: Action) {
        this.me = me
        this.rules = rules
        this.game = game
        this.action = action
        isGk = me.id == teammates.goalkeeper.id
    }

    override fun customRendering(): String {
        val localTeammates = teammates
        val goalkeeper = teammates.goalkeeper
        val teammateDebugInfo = localTeammates
                .flatMap {
                    val r = if (it.id == goalkeeper.id) 0.0 else 1.0
                    val g = if (it.id == goalkeeper.id) 1.0 else 0.0
                    val b = 0.0
                    setOf(
                            Text("${it.id} (gk: ${it.id == localTeammates.goalkeeper.id}): {x:${"%.2f".format(it.x)} y:${"%.2f".format(it.y)} z:${"%.2f".format(it.z)}}"),
                            LineContainer(Line(it.x, it.y, it.z,
                                    it.x + it.velocity_x, it.y + it.velocity_y, it.z + it.velocity_z)),
                            SphereContainer(Sphere(it.x + it.radius, it.y + it.radius, it.z + it.radius,
                                    0.1, r, g, b)), //marker green - goalkeeper, red - forward
                            SphereContainer(Sphere(targetX, 0.0, targetZ,
                                    0.5, 0.0, 1.0, 0.0))) //target of me
                }

        val opponentsDebugInfo = opponents
                .flatMap { opponent ->
                    val predictedPosition = opponent.toActiveObject().predictedPosition(1.0)
                    val predictedKick = predictedCollision(opponent.toActiveObject(), game.ball.toActiveObject())
                    val setOfDebug = mutableSetOf(
                            LineContainer(Line(opponent.toActiveObject().position, predictedPosition, 0.5, 1.0, 0.0, 0.0)),
                            SphereContainer(Sphere(predictedPosition, 0.5, 1.0, 0.0, 0.0)))
                    predictedKick?.let {
                        setOfDebug.add(SphereContainer(Sphere(it, 0.75, 1.0, 1.0, 0.0)))
                        setOfDebug.add(Text("Robot: ${opponent.id} are going to kick from v$it"))
                    }
                    setOfDebug
                }

        val predictedBallPosition = game.ball.toActiveObject().predictedPosition(timeToMeetingBallWithGk())
        val commonDebugInfo = listOf(
                Text(game.current_tick.toString()),
                LineContainer(Line(game.ball.toActiveObject().position, predictedBallPosition, 0.5, 0.0, 0.0, 1.0)),
                SphereContainer(Sphere(predictedBallPosition, 0.5, 0.0, 0.0, 1.0)))

        return gson.toJson(teammateDebugInfo + commonDebugInfo + opponentsDebugInfo)
    }

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

}

private infix fun Point.middle(another: Point): Point {
    return Point((this.x - another.x) / 2, (this.y - another.y) / 2, (this.z - another.z) / 2)
}

fun Ball.toActiveObject() = ActiveObject(this)

fun Robot.toActiveObject() = ActiveObject(this)

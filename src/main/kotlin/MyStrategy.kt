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

    var isGk: Boolean = false

    val targetPostions = HashMap<Int, Point>()

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
        val ballMeetingTimeX = timeToMeetingManagedAndUnmanagedObjects(teammates.goalkeeper.toActiveObject(), game.ball.toActiveObject())

        val predictedCollision = opponents.map { predictedCollision(it.toActiveObject(), game.ball.toActiveObject()) }.filterNotNull().firstOrNull()

        val predictedBallPosition = game.ball.toActiveObject().predictedPosition(ballMeetingTimeX)
        val rawTargetX = predictedCollision?.x ?: predictedBallPosition.x
        val targetPosition = Point(adjustGoalkeeperXPosition(rawTargetX), 0.0, goalZ)
        targetPostions[me.id] = targetPosition

        val targetXDif = targetPosition.x - me.x
        val targetZDif = targetPosition.z - me.z

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
        return gson.toJson(collectDebugInfo(teammates, opponents, game, targetPostions))
    }

}

private infix fun Point.middle(another: Point): Point {
    return Point((this.x - another.x) / 2, (this.y - another.y) / 2, (this.z - another.z) / 2)
}

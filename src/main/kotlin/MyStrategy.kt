import com.google.gson.GsonBuilder
import model.*
import kotlin.math.*

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
        get() = game.robots.opponents

    var isGk: Boolean = false

    val targetPostions = HashMap<Int, Point>()

    val maxGoalX by lazy { rules.arena.goal_width / 2 - rules.arena.bottom_radius }
    val minGoalX by lazy { -rules.arena.goal_width / 2 + rules.arena.bottom_radius }
    val goalZ by lazy { -rules.arena.depth / 2 }

    lateinit var potentialFields: PotentialFields

    var forwardsAreWaiting = false


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

        val gkActiveObject = teammates.goalkeeper.toActiveObject()
        val ballActiveObject = game.ball.toActiveObject()

        val gkBallMeetingTime = timeToMeetingManagedAndUnmanagedObjects(gkActiveObject, ballActiveObject)
        val predictedBallPosition = ballActiveObject.predictedPosition(gkBallMeetingTime, rules.arena)

        val predictedCollision = opponents.mapNotNull { predictedCollision(it.toActiveObject(), ballActiveObject) }.firstOrNull()
        val outGkBallMeetingTime = timeToMeetingRunningRobotAndUnmanagedObjects(gkActiveObject, ballActiveObject, rules)

        val forwards = teammates.filter { it.id != me.id }
        if (forwardsAreWaiting ||
                game.ball.z < 0 // ball in on our side
                && hypot(game.ball.velocity_x, game.ball.velocity_z) > game.ball.velocity_y // ball is not taking off
                && (distance(gkActiveObject, ballActiveObject) < game.ball.radius * 3 || forwards.none { it.z < 0 }) //all teammates are on other size
                && outGkBallMeetingTime < MAX_PREDICTED_TIME
                && (predictedCollision == null || outGkBallMeetingTime < predictedCollision.second)) {
            //go to knock out
            val knokingOutPosition = ballActiveObject.predictedPosition(outGkBallMeetingTime, rules.arena)
            targetPostions[me.id] = knokingOutPosition
        } else { //hold goals
            val rawTargetX = predictedCollision?.first?.x ?: predictedBallPosition.x
            val targetPosition = Point(adjustGoalkeeperXPosition(rawTargetX), 0.0, goalZ)
            targetPostions[me.id] = targetPosition
        }

        goToTargetPosition()
        processMovingActionAccordingToPotentialFields()
        kickIfPossible(predictedBallPosition)
    }

    private fun kickIfPossible(predictedBallPosition: Point) {
        if ((me.x - game.ball.x).absoluteValue < (game.ball.radius + me.radius) * 1.1 && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25) {
            if (me.z < game.ball.z) {
                action.jump_speed = computeJumpSpeed()
                val distanceToBallX = predictedBallPosition.x - me.x
                val distanceToBallZ = predictedBallPosition.z - me.z
                action.target_velocity_x = distanceToBallX
                action.target_velocity_z = distanceToBallZ
            }
        }
    }

    private fun goToTargetPosition() {
        val targetPosition = targetPostions[me.id]!!

        val targetXDif = targetPosition.x - me.x
        val targetZDif = targetPosition.z - me.z
        val maxDiff = max(targetXDif, targetZDif)
        val diffPerSpeedUnit = (maxDiff / rules.ROBOT_MAX_GROUND_SPEED).absoluteValue

        action.target_velocity_x = if (targetXDif.absoluteValue >= 2) targetXDif / diffPerSpeedUnit.safeZero() else (if (targetXDif.absoluteValue < 0.05) 0.0 else targetXDif.sign() * rules.ROBOT_MAX_GROUND_SPEED / 4)
        action.target_velocity_z = if (targetZDif.absoluteValue >= 2) targetZDif / diffPerSpeedUnit.safeZero() else (if (targetZDif.absoluteValue < 0.05) 0.0 else targetZDif.sign() * rules.ROBOT_MAX_GROUND_SPEED / 4)
    }

    private fun adjustGoalkeeperXPosition(rawTargetX: Double) = min(max(rawTargetX, minGoalX), maxGoalX)

    private fun manageForwards() {
        if (isGk) return
        val ballActiveObject = game.ball.toActiveObject()

        val forwardBallMeetingTime = timeToMeetingManagedAndUnmanagedObjects(me.toActiveObject(), ballActiveObject)
        val predictedBallPosition = ballActiveObject.predictedPosition(forwardBallMeetingTime, rules.arena)
        val bestPositionToKick = findBestPositionToKick(if (predictedBallPosition.y > me.radius * 2) ballActiveObject.position else predictedBallPosition)
        val allOpponentsAreOnTheirSide = opponents.map { it.z }.all { it > 0 }
        forwardsAreWaiting = false
        val zPos = if (allOpponentsAreOnTheirSide) {
            if (bestPositionToKick.z < -rules.arena.depth / 5) {
                forwardsAreWaiting = true
                -rules.arena.depth / 5
            } else {
                bestPositionToKick.z
            }
        } else bestPositionToKick.z
        targetPostions[me.id] = Point(bestPositionToKick.x, 0.0, zPos)
        println("Tick: ${game.current_tick}")
        println("Forward ball meeting time: $forwardBallMeetingTime")
        println("Predicted ball position: $predictedBallPosition")
        println("Forward target pos: ${targetPostions[me.id]}")

        goToTargetPosition()
        processMovingActionAccordingToPotentialFields()
        kickIfPossible(predictedBallPosition)
    }

    private fun findBestPositionToKick(predictedBallPosition: Point): Point {
        val (ballX, _, ballZ) = predictedBallPosition
        val opponentGoalX = rules.arena.goal_width / 2
        val opponentGoalZ = rules.arena.depth / 2 + game.ball.radius
        val targetBallXDiff = opponentGoalX - ballX
        val targetBallZDiff = opponentGoalZ - ballZ
        val xzProportion = (targetBallXDiff / targetBallZDiff.safeZero()).absoluteValue
        val hypot = game.ball.radius + me.radius
        val x2z2 = sqrt(hypot) // x ^ 2 + z ^ 2  => x ^ 2 + xzProportion ^ 2 * x ^ 2 => x ^ 2 ( 1 + xzProportion)
        val x = sqrt(x2z2 / (1 + xzProportion))
        val z = x / xzProportion.safeZero()
        return Point(ballX + game.ball.velocity_x.sign * x, 0.0, ballZ - z)
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
        targetPostions.clear()
    }

    override fun customRendering(): String {
        return gson.toJson(collectDebugInfo(teammates, opponents, game, rules, targetPostions, potentialFields))
    }


    private fun processMovingActionAccordingToPotentialFields() {
        potentialFields = PotentialFields(me, game, rules, action)
        potentialFields.modifyWayPointsByCurrentTargetPosition(targetPostions[me.id]!!)
    }
}

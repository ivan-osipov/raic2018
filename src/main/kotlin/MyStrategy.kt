import RobotState.*
import com.google.gson.GsonBuilder
import model.Action
import model.Game
import model.Robot
import model.Rules
import kotlin.math.*

class MyStrategy : Strategy {

    //constants
    private val gson = GsonBuilder().create()
    val maxGoalX by lazy { rules.arena.goal_width / 2 - rules.arena.bottom_radius }
    val minGoalX by lazy { -rules.arena.goal_width / 2 + rules.arena.bottom_radius }
    val goalZ by lazy { -rules.arena.depth / 2 }
    val frontGoalPoint by lazy { Vector3d((maxGoalX + minGoalX) / 2, me.radius, goalZ) }

    //tick data
    lateinit var me: Robot
    lateinit var rules: Rules
    lateinit var game: Game
    lateinit var action: Action
    lateinit var simulator: Simulator

    //computed data
    val states = HashMap<Int, RobotState>()
    var predictedWorldStates: List<WorldState> = ArrayList()
    val predictedBallPositions: List<Vector3d>
        get() {
            return predictedWorldStates.map(WorldState::ball).map(Entity::position)
        }
    val predictedRobotState: Map<Int, List<Entity>>
        get() {
            return predictedWorldStates.asSequence().flatMap { it -> it.robots.asSequence() }.groupBy { it.id }
        }
    var lastPredictingTick = -1
    val targetPositions = HashMap<Int, Vector3d>()
    val entitiesByRobotIds: MutableMap<Int, Entity> = HashMap()
    lateinit var teammates: List<Robot>
    lateinit var opponents: List<Robot>
    lateinit var ballEntity: Entity
    lateinit var myEntity: Entity

    var forwardsAreWaiting = false
    lateinit var potentialFields: PotentialFields
    var initialized: Boolean = false

    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        init(me, rules, game, action)
        doBehaviour()
    }

    private fun init(me: Robot, rules: Rules, game: Game, action: Action) {
        this.me = me
        this.rules = rules
        this.game = game
        this.action = action
        this.simulator = Simulator(rules)

        teammates = game.robots.teammates
        opponents = game.robots.opponents

        entitiesByRobotIds.clear()
        for (robot in game.robots) {
            entitiesByRobotIds[robot.id] = robot.toEntity(rules)
        }
        myEntity = entitiesByRobotIds[me.id]!!

        if (states.isEmpty()) {
            val (teammate1, teammate2) = teammates
                    .sortedBy { distance(entitiesByRobotIds[it.id]!!.position, frontGoalPoint) }
            states[teammate1.id] = DEFENCE
            states[teammate2.id] = ATTACK
        }


        targetPositions.clear()
        for (teammate in teammates) {
            targetPositions[teammate.id] = entitiesByRobotIds[teammate.id]!!.position
        }

        if (lastPredictingTick != game.current_tick) {
            ballEntity = game.ball.toEntity(rules)
            predictedWorldStates = simulator.predictedWorldStates(WorldState(entitiesByRobotIds.values.toList(), ballEntity))
            lastPredictingTick = game.current_tick
        }

        initialized = true
    }

    private fun doBehaviour() {
        when (states[me.id]) {
            ATTACK -> attack()
            DEFENCE -> defence()
            RUNNING_ON_POSITION -> println()
            KNOCKING_OUT -> println()
            WAITING -> println()
        }
    }

    private fun defence() {

        val predictedBallPositionIndexBeforeMyGoal = simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)

        val outGkBallMeetingTime = simulator.timeToMeetingManagedAndUnmanagedObjects(myEntity, ballEntity)

        val forwards = teammates.filter { it.id != me.id }
        if (forwardsAreWaiting ||
                game.ball.z < 0 // ball in on our side
                && hypot(game.ball.velocity_x, game.ball.velocity_z) > game.ball.velocity_y // ball is not taking off
                && (distance(myEntity, ballEntity) < game.ball.radius * 3 || forwards.none { it.z < 0 }) //all teammates are on other size
                && outGkBallMeetingTime < simulator.MAX_PREDICTED_TIME) {
            //go to knock out
            if(game.ball.velocity_z < 0) {
                val knockOutStateIndex = simulator.predictedPositionIndexBeforeMyGoal(predictedBallPositions)
                val targetPositionIndex = knockOutStateIndex ?: clamp(10, 0, predictedBallPositions.size - 1)
                val worldStateOnKnockingOut = predictedWorldStates[targetPositionIndex]
                targetPositions[me.id] = worldStateOnKnockingOut.ball.position
            } else {
                targetPositions[me.id] = ballEntity.position
            }
        } else if (predictedBallPositionIndexBeforeMyGoal != null) { //hold goals
            val coef = distance(myEntity, ballEntity) / distance(myEntity, predictedWorldStates[predictedBallPositionIndexBeforeMyGoal].ball)
            val (x, y, z) = predictedBallPositions[clamp(ceil(predictedBallPositionIndexBeforeMyGoal * coef).toInt(), 0, predictedBallPositionIndexBeforeMyGoal)]
            val targetPosition = Vector3d(x, y, z)
            targetPositions[me.id] = targetPosition
            if(distance(ballEntity.position, frontGoalPoint) <= distance(myEntity.position, frontGoalPoint)
                    && (ballEntity.velocity.y > 0 || ballEntity.position.y > (ballEntity.radius + myEntity.radius))) {
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED
            }
        } else { //hold goals
            targetPositions[me.id] = Vector3d(clamp(ballEntity.position.x, minGoalX, maxGoalX), myEntity.radius, goalZ)
        }

        goToTargetPosition()
        processMovingActionAccordingToPotentialFields()
        kickIfPossible()
    }

    private fun kickIfPossible() {
        if ((me.x - game.ball.x).absoluteValue < (game.ball.radius + me.radius) * 1.1 && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25) {
            if (me.z < game.ball.z) {
                action.jump_speed = computeJumpSpeed()
                action.target_velocity_x = 0.0
                action.target_velocity_z = 0.0
            }
        }
    }

    private fun goToTargetPosition() {
        val targetPosition = targetPositions[me.id]!!
        val diff = targetPosition - myEntity.position
        val length = diff.length()
        val velocity = if(length >= 2) rules.ROBOT_MAX_GROUND_SPEED else if(length < 0.1) 0.0 else rules.ROBOT_MAX_GROUND_SPEED / 4
        val (vX, vY, vZ) = diff.normalize() * velocity

        action.target_velocity_x = vX
        action.target_velocity_y = vY
        action.target_velocity_z = vZ
    }

    private fun attack() {
        val forwardBallMeetingTime = simulator.timeToMeetingManagedAndUnmanagedObjects(myEntity, ballEntity)
        val index = ceil(forwardBallMeetingTime / simulator.deltaTime()).toInt()
        val predictedBallPosition = predictedBallPositions[clamp(index, 0, predictedBallPositions.size - 1)]
        val bestPositionToKick = findBestPositionToKick(if (predictedBallPosition.y > me.radius * 2) ballEntity.position else predictedBallPosition)
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
        targetPositions[me.id] = Vector3d(bestPositionToKick.x, 0.0, zPos)
        if (false) {
            println("Tick: ${game.current_tick}")
            println("Forward ball meeting time: $forwardBallMeetingTime")
            println("Predicted ball position: $predictedBallPosition")
            println("Forward target pos: ${targetPositions[me.id]}")
        }

        goToTargetPosition()
        processMovingActionAccordingToPotentialFields()
        kickIfPossible()
    }

    private fun findBestPositionToKick(predictedBallPosition: Vector3d): Vector3d {
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
        return Vector3d(ballX + game.ball.velocity_x.sign * x, 0.0, ballZ - z)
    }

    private fun computeJumpSpeed(): Double {
        if (game.ball.z < 0) {
            return rules.ROBOT_MAX_JUMP_SPEED
        } else {
            return 0.8 * rules.ROBOT_MAX_JUMP_SPEED
        }
    }

    private fun processMovingActionAccordingToPotentialFields() {
        potentialFields = PotentialFields(me, game, rules, action)
        potentialFields.modifyWayPointsByCurrentTargetPosition(targetPositions[me.id]!!)
    }

    override fun customRendering(): String {
        return gson.toJson(collectDebugInfo(this))
    }
}

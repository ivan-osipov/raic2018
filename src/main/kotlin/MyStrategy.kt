import RobotState.*
import com.google.gson.GsonBuilder
import model.Action
import model.Game
import model.Robot
import model.Rules

class MyStrategy : Strategy {

    //constants
    private val gson = GsonBuilder().create()
    val maxGoalX by lazy { rules.arena.goal_width / 2 - rules.arena.bottom_radius }
    val minGoalX by lazy { -rules.arena.goal_width / 2 + rules.arena.bottom_radius }
    val goalZ by lazy { -rules.arena.depth / 2 }
    val opponentGoalZ by lazy { rules.arena.depth / 2 }
    val frontGoalPoint by lazy { Vector3d((maxGoalX + minGoalX) / 2, me.radius, goalZ) }
    val frontOpponentGoalPoint by lazy { Vector3d((maxGoalX + minGoalX) / 2, me.radius, opponentGoalZ) }

    //tick data
    lateinit var me: Robot
    lateinit var rules: Rules
    lateinit var game: Game
    lateinit var action: Action
    lateinit var simulator: Simulator
    lateinit var stateDefinitionHelper: StateDefinitionHelper

    //computed data
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
    var states: Map<Int, RobotState> = HashMap()
    var internalStates: MutableMap<Int, Any> = HashMap()
    lateinit var teammates: List<Robot>
    lateinit var opponents: List<Robot>
    lateinit var ballEntity: Entity
    lateinit var myEntity: Entity

    //    lateinit var potentialFields: PotentialFields
    var score = "0x0"
    var ballIsPlayed = false

    override fun act(me: Robot, rules: Rules, game: Game, action: Action) {
        init(me, rules, game, action)
        doBehaviour()
    }

    private fun init(me: Robot, rules: Rules, game: Game, action: Action) {
        this.me = me
        this.rules = rules
        this.game = game
        this.action = action

        teammates = game.robots.teammates
        opponents = game.robots.opponents

        entitiesByRobotIds.clear()
        for (robot in game.robots) {
            entitiesByRobotIds[robot.id] = robot.toEntity(rules)
        }
        myEntity = entitiesByRobotIds[me.id]!!

        targetPositions.clear()
        for (teammate in teammates) {
            targetPositions[teammate.id] = entitiesByRobotIds[teammate.id]!!.position
        }

        this.simulator = Simulator(this)

        if (lastPredictingTick != game.current_tick) {
            ballEntity = game.ball.toEntity(rules)
            predictedWorldStates = simulator.predictedWorldStates(WorldState(
                    game.current_tick,
                    ballEntity,
                    entitiesByRobotIds.values.toList()))
            lastPredictingTick = game.current_tick
        }

        this.stateDefinitionHelper = StateDefinitionHelper(this)
        states = stateDefinitionHelper.computeStateMap()

        val newScore = game.players.toList().sortedByDescending { it.me }.joinToString("x") { it.score.toString() }
        if(newScore != score) {
            score = newScore
            ballIsPlayed = false
            println(score)
        } else {
            ballIsPlayed = ballEntity.position.x != 0.0 || ballEntity.position.z != 0.0
        }
    }

    private fun doBehaviour() {
        when (states[me.id]) {
            ATTACK -> AttackBehaviour(this)
            DEFENCE -> DefenceBehaviour(this)
            KNOCKING_OUT -> KnockOutBehaviour(this)
            ACTIVE_DEFENCE ->   ActiveDefenceBehaviour(this)
            else -> null
        }?.apply {
            doIt()
            internalStates[me.id] = getCurrentState()
        }
    }

//    fun processMovingActionAccordingToPotentialFields() {
//        potentialFields = PotentialFields(me, game, rules, action)
//        potentialFields.modifyWayPointsByCurrentTargetPosition(targetPositions[me.id]!!)
//    }

    override fun customRendering(): String {
        return gson.toJson(collectDebugInfo(this))
    }
}

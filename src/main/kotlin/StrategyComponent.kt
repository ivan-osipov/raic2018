import model.Action
import model.Game
import model.Robot
import model.Rules

abstract class StrategyComponent(val strategy: MyStrategy) {
    val me: Robot by lazy { strategy.me }
    val rules: Rules by lazy { strategy.rules }
    val game: Game by lazy { strategy.game }
    val action: Action by lazy { strategy.action }

    val frontGoalPoint by lazy { strategy.frontGoalPoint }
    val frontOpponentGoalPoint by lazy { strategy.frontOpponentGoalPoint }
    val minGoalX by lazy { strategy.minGoalX }
    val maxGoalX by lazy { strategy.maxGoalX }
    val goalZ by lazy { strategy.goalZ }

    val simulator by lazy { strategy.simulator }
    val entitiesByRobotIds by lazy { strategy.entitiesByRobotIds }
    val predictedBallPositions by lazy { strategy.predictedBallPositions }
    val myEntity by lazy { strategy.myEntity }
    val ballEntity by lazy { strategy.ballEntity }
    val teammates by lazy { strategy.teammates }
    val targetPositions by lazy { strategy.targetPositions }
    val predictedWorldStates by lazy { strategy.predictedWorldStates }
    val opponents by lazy { strategy.opponents }
}
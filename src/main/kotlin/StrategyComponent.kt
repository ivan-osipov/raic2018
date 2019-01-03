import model.Action
import model.Game
import model.Robot
import model.Rules

abstract class StrategyComponent(val strategy: MyStrategy) {
    val me: Robot = strategy.me
    val rules: Rules = strategy.rules
    val game: Game = strategy.game
    val action: Action = strategy.action

    val frontGoalPoint = strategy.frontGoalPoint
    val minGoalX = strategy.minGoalX
    val maxGoalX = strategy.maxGoalX
    val goalZ = strategy.goalZ

    val simulator = strategy.simulator
    val entitiesByRobotIds = strategy.entitiesByRobotIds
    val predictedBallPositions = strategy.predictedBallPositions
    val myEntity = strategy.myEntity
    val ballEntity = strategy.ballEntity
    val teammates = strategy.teammates
    val targetPositions = strategy.targetPositions
    val predictedWorldStates = strategy.predictedWorldStates
    val opponents = strategy.opponents
}
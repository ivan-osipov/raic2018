import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.sqrt

abstract class AbstractBehaviour(strategy: MyStrategy) : StrategyComponent(strategy) {

    protected abstract fun perform()

    fun doIt() {
        perform()
    }

    protected fun kickIfPossible() {
        if ((strategy.me.x - strategy.game.ball.x).absoluteValue < (strategy.game.ball.radius + strategy.me.radius) * 1.1 && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25) {
            if (strategy.me.z < strategy.game.ball.z) {
                strategy.action.jump_speed = computeJumpSpeed()
                strategy.action.target_velocity_x = 0.0
                strategy.action.target_velocity_z = 0.0
            }
        }
    }

    protected fun kickIt(kickingEntityPosition: Vector3d) {
        val velocity = (kickingEntityPosition - myEntity.position).normalize() * rules.ROBOT_MAX_GROUND_SPEED
        strategy.action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED / 2
        strategy.action.target_velocity_x = velocity.x
        strategy.action.target_velocity_y = velocity.y
        strategy.action.target_velocity_z = velocity.z
    }

    protected fun goToTargetPosition() {
        val targetPosition = strategy.targetPositions[me.id]!!
        val diff = targetPosition - strategy.myEntity.position
        val length = diff.length()
        val velocity = if (length >= 2) rules.ROBOT_MAX_GROUND_SPEED else if (length < 0.1) 0.0 else rules.ROBOT_MAX_GROUND_SPEED / 4
        val (vX, vY, vZ) = diff.normalize() * velocity

        action.target_velocity_x = vX
        action.target_velocity_y = vY
        action.target_velocity_z = vZ
    }

    protected fun computeJumpSpeed(): Double {
        if (game.ball.z < 0) {
            return rules.ROBOT_MAX_JUMP_SPEED
        } else {
            return 0.8 * rules.ROBOT_MAX_JUMP_SPEED
        }
    }


    protected fun findBestPositionToKick(predictedBallPosition: Vector3d): Vector3d {
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
}
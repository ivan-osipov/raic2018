import kotlin.math.absoluteValue

abstract class AbstractBehaviour<T>(strategy: MyStrategy) : StrategyComponent(strategy) {

    protected abstract fun perform()

    fun doIt() {
        perform()
    }

    protected fun kickBall() {
        kickIt(ballEntity.position)
    }

    protected fun kickIfPossible() {
        if ((strategy.me.x - strategy.game.ball.x).absoluteValue < (strategy.game.ball.radius + strategy.me.radius) * 1.1 && (me.z - game.ball.z).absoluteValue < (game.ball.radius + me.radius) * 1.25) {
            if (strategy.me.z < strategy.game.ball.z) {
                kickIt(strategy.ballEntity.position)
            }
        }
    }


    abstract fun getCurrentState(): T

    protected fun kickIt(kickingEntityPosition: Vector3d) {
        val (x, y, z) = (kickingEntityPosition - myEntity.position).normalize() * rules.ROBOT_MAX_GROUND_SPEED
        strategy.action.jump_speed = computeJumpSpeed(myEntity.position.z)
        strategy.action.target_velocity_x = x
        strategy.action.target_velocity_y = y
        strategy.action.target_velocity_z = z
    }

    protected fun goToTargetPosition(slowdown: Boolean = true) {
        val targetPosition = strategy.targetPositions[me.id]!!
        val diff = targetPosition - strategy.myEntity.position
        val distance = diff.length()
        val velocityValue = myEntity.velocity.length()
        val stopTime = velocityValue / rules.ROBOT_ACCELERATION
        val velocity = if (slowdown && distance / velocityValue <= stopTime) 0.0 else rules.ROBOT_MAX_GROUND_SPEED
        val (vX, vY, vZ) = diff.normalize() * velocity

        action.target_velocity_x = vX
        action.target_velocity_y = vY
        action.target_velocity_z = vZ
    }

    protected fun computeJumpSpeed(z: Double): Double {
        if (z < 0) {
            return rules.ROBOT_MAX_JUMP_SPEED
        } else {
            return 0.8 * rules.ROBOT_MAX_JUMP_SPEED
        }
    }
}
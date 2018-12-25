import model.Ball
import model.Robot
import kotlin.math.pow
import kotlin.math.sqrt

class ActiveObject {
    val x: Double
    val y: Double
    val z: Double
    val velocity_x: Double
    val velocity_y: Double
    val velocity_z: Double
    val radius: Double

    constructor(ball: Ball) {
        this.x = ball.x
        this.y = ball.y
        this.z = ball.z
        this.velocity_x = ball.velocity_x
        this.velocity_y = ball.velocity_y
        this.velocity_z = ball.velocity_z
        this.radius = ball.radius
    }

    constructor(robot: Robot) {
        this.x = robot.x
        this.y = robot.y
        this.z = robot.z
        this.velocity_x = robot.velocity_x
        this.velocity_y = robot.velocity_y
        this.velocity_z = robot.velocity_z
        this.radius = robot.radius
    }

    fun velocity() = sqrt(velocity_x.pow(2) + velocity_y.pow(2) + velocity_z.pow(2))

    val position
        get() = Point(x, y, z)
}

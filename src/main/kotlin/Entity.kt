import model.Ball
import model.Robot
import model.Rules

open class Entity {
    var position: Vector3d
    var velocity: Vector3d
    val radius: Double
    val mass: Double
    val arenaE: Double

    constructor(original: Entity) {
        this.position = original.position.copy()
        this.velocity = original.velocity.copy()
        this.radius = original.radius
        this.mass = original.mass
        this.arenaE = original.arenaE
    }

    constructor(ball: Ball, rules: Rules) {
        this.position = Vector3d(ball.x, ball.y, ball.z)
        this.velocity = Vector3d(ball.velocity_x, ball.velocity_y, ball.velocity_z)
        this.radius = ball.radius
        this.mass = rules.BALL_MASS
        this.arenaE = rules.BALL_ARENA_E
    }

    constructor(robot: Robot, rules: Rules) {
        this.position = Vector3d(robot.x, robot.y, robot.z)
        this.velocity = Vector3d(robot.velocity_x, robot.velocity_y, robot.velocity_z)
        this.radius = robot.radius
        this.mass = rules.ROBOT_MASS
        this.arenaE = rules.ROBOT_ARENA_E
    }
}

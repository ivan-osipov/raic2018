import model.Ball
import model.Robot
import model.Rules

open class Entity {
    val id: Int
    var position: Vector3d
    var velocity: Vector3d
    val radius: Double
    val mass: Double
    val arenaE: Double
    val radiusChangeSpeed: Double

    constructor(original: Entity) {
        this.id = original.id
        this.position = original.position.copy()
        this.velocity = original.velocity.copy()
        this.radius = original.radius
        this.mass = original.mass
        this.arenaE = original.arenaE
        this.radiusChangeSpeed = original.radiusChangeSpeed
    }

    constructor(ball: Ball, rules: Rules) {
        this.id = -1
        this.position = Vector3d(ball.x, ball.y, ball.z)
        this.velocity = Vector3d(ball.velocity_x, ball.velocity_y, ball.velocity_z)
        this.radius = ball.radius
        this.mass = rules.BALL_MASS
        this.arenaE = rules.BALL_ARENA_E
        this.radiusChangeSpeed = 0.0
    }

    constructor(robot: Robot, rules: Rules) {
        this.id = robot.id
        this.position = Vector3d(robot.x, robot.y, robot.z)
        this.velocity = Vector3d(robot.velocity_x, robot.velocity_y, robot.velocity_z)
        this.radius = robot.radius
        this.mass = rules.ROBOT_MASS
        this.arenaE = rules.ROBOT_ARENA_E
        this.radiusChangeSpeed = rules.ROBOT_MAX_JUMP_SPEED / 2
    }
}

import model.Rules
import java.util.*
import kotlin.math.ceil

class Simulator(val rules: Rules) {
    val MAX_PREDICTED_TIME = 3.0

    private val arena = rules.arena

    fun predictedPositionIndexBeforeMyGoal(positions: List<Vector3d>): Int? {
        for (i in 0..(positions.size - 1)) {
            val position = positions[i]
            if (position.z <= -rules.arena.depth / 2 + rules.BALL_RADIUS) {
                return i
            }
        }
        return null
    }

    fun predictedWorldStates(worldState: WorldState): List<WorldState> {
        val deltaTime = deltaTime()
        val ticksCount = ceil(MAX_PREDICTED_TIME / deltaTime).toInt()

        var currentWorldState = worldState
        val predictedWorldStates = ArrayList<WorldState>()
        for (tick in 0..ticksCount) {
            val robots = currentWorldState.robots.map { move(it, deltaTime) }.toMutableList()
            var ball = move(currentWorldState.ball, deltaTime)

            for (i in 0 until robots.size) {
                for (j in 0 until i) {
                    val (robotI, robotJ) = collideEntities(robots[i], robots[j]) ?: continue
                    robots[i] = robotI
                    robots[j] = robotJ
                }
            }

            for (i in 0 until robots.size) {
                val collideResult = collideEntities(robots[i], ball)
                if (collideResult != null) {
                    val (newRobotState, newBallState) = collideResult
                    ball = newBallState
                    robots[i] = newRobotState
                }

                robots[i] = collideWithArena(robots[i]) ?: continue
            }

            ball = collideWithArena(ball) ?: ball

            currentWorldState = WorldState(robots, ball)

            predictedWorldStates.add(currentWorldState)
        }

        return predictedWorldStates
    }

    fun timeToMeetingManagedAndUnmanagedObjects(managedObject: Entity,
                                                unmanagedObject: Entity): Double {
        val distance = distance(managedObject, unmanagedObject)
        return distance / rules.ROBOT_MAX_GROUND_SPEED
    }

    fun collideEntities(a: Entity, b: Entity): Pair<Entity, Entity>? {
        val deltaPosition = b.position - a.position
        val distance = deltaPosition.length()
        val penetration = a.radius + b.radius - distance
        if (penetration > 0) {
            val normal = deltaPosition.normalize()
            val coefA = (1 / a.mass) / ((1 / a.mass) + (1 / b.mass))
            val coefB = (1 / b.mass) / ((1 / a.mass) + (1 / b.mass))
            val aCopy = Entity(a)
            val bCopy = Entity(b)
            aCopy.position -= normal * penetration * coefA
            bCopy.position += normal * penetration * coefB
            val deltaVelocity = (b.velocity - a.velocity) * normal - b.radiusChangeSpeed - a.radiusChangeSpeed
            if (deltaVelocity < 0) {
                val impulse = normal * deltaVelocity * (1 + random(rules.MIN_HIT_E, rules.MAX_HIT_E))
                a.velocity += impulse * coefA
                b.velocity -= impulse * coefB
            }
            return Pair(a, b)
        }
        return null
    }

    private fun random(rangeMin: Double, rangeMax: Double): Double {
        val r = Random(rules.seed)
        return rangeMin + (rangeMax - rangeMin) * r.nextDouble()
    }

    fun collideWithArena(entity: Entity): Entity? {
        val (distance, normal) = danToArena(entity.position)
        val penetration = entity.radius - distance
        if (penetration > 0) {
            val copy = Entity(entity)
            copy.position += normal * penetration
            val velocity = copy.velocity * normal //- e.radius_change_speed
            if (velocity < 0) {
                copy.velocity -= (normal * velocity * (1 + entity.arenaE))
                return copy
            }
        }
        return null
    }

    fun move(entity: Entity, deltaTime: Double): Entity {
        val copy = Entity(entity)
        copy.position += copy.velocity * deltaTime
        copy.position.y -= rules.GRAVITY * deltaTime * deltaTime / 2
        copy.velocity.y -= rules.GRAVITY * deltaTime
        return copy
    }

    fun deltaTime(sec: Double = MAX_PREDICTED_TIME): Double {
        return sec / 50
    }

    fun danToArena(point: Vector3d): Dan {
        val copiedPoint = point.copy()
        val negateX = point.x < 0
        val negateZ = point.z < 0
        if (negateX) {
            copiedPoint.x = -point.x
        }
        if (negateZ) {
            copiedPoint.z = -point.z
        }
        val result = danToArenaQuarter(copiedPoint)
        if (negateX) {
            result.normal.x = -result.normal.x
        }
        if (negateZ) {
            result.normal.z = -result.normal.z
        }
        return result
    }

    private fun danToPlane(point: Vector3d,
                           pointOnPlane: Vector3d,
                           planeNormal: Vector3d): Dan {
        return Dan(
                (point - pointOnPlane) * planeNormal,
                planeNormal
        )
    }

    private fun danToSphereInner(point: Vector3d,
                                 sphereCenter: Vector3d,
                                 sphereRadius: Double): Dan {
        return Dan(
                sphereRadius - (point - sphereCenter).length(),
                (sphereCenter - point).normalize()
        )
    }

    private fun danToSphereOuter(point: Vector3d,
                                 sphereCenter: Vector3d,
                                 sphereRadius: Double): Dan {
        return Dan(
                (point - sphereCenter).length() - sphereRadius,
                (point - sphereCenter).normalize()
        )
    }

    private fun danToArenaQuarter(point: Vector3d): Dan {
        // Ground
        var dan = danToPlane(point, Vector3d(0.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0))

        // Ceiling
        dan = minOf(dan, danToPlane(point, Vector3d(0.0, arena.height, 0.0), Vector3d(0.0, -1.0, 0.0)))

        // Side x
        dan = minOf(dan, danToPlane(point, Vector3d(arena.width / 2, 0.0, 0.0), Vector3d(-1.0, 0.0, 0.0)))

        // Side z (goal)
        dan = minOf(dan, danToPlane(point, Vector3d(0.0, 0.0, (arena.depth / 2) + arena.goal_depth), Vector3d(0.0, 0.0, -1.0)))
        // Side z
        val v = Vector2d(point.x, point.y) -
                Vector2d(arena.goal_width / 2 - arena.goal_top_radius, arena.goal_height - arena.goal_top_radius)
        if (point.x >= (arena.goal_width / 2) + arena.goal_side_radius
                || point.y >= arena.goal_height + arena.goal_side_radius
                || (v.x > 0 && v.y > 0 && v.length() >= arena.goal_top_radius + arena.goal_side_radius)) {
            dan = minOf(dan, danToPlane(point, Vector3d(0.0, 0.0, arena.depth / 2), Vector3d(0.0, 0.0, -1.0)))
        }

        // Side z & ceiling (goal)
        if (point.z >= (arena.depth / 2) + arena.goal_side_radius) {
            // x
            dan = minOf(dan, danToPlane(point, Vector3d(arena.goal_width / 2, 0.0, 0.0), Vector3d(-1.0, 0.0, 0.0)))
            // y
            dan = minOf(dan, danToPlane(point, Vector3d(0.0, arena.goal_height, 0.0), Vector3d(0.0, -1.0, 0.0)))
        }

        // Goal back corners
        if (point.z > (arena.depth / 2) + arena.goal_depth - arena.bottom_radius) {
            dan = minOf(dan, danToSphereInner(
                    point,
                    Vector3d(
                            clamp(point.x, arena.bottom_radius - (arena.goal_width / 2), (arena.goal_width / 2) - arena.bottom_radius),
                            clamp(point.y, arena.bottom_radius, arena.goal_height - arena.goal_top_radius),
                            (arena.depth / 2) + arena.goal_depth - arena.bottom_radius
                    ),
                    arena.bottom_radius
            ))
        }

        // Corner
        if (point.x > (arena.width / 2) - arena.corner_radius
                && point.z > (arena.depth / 2) - arena.corner_radius) {
            dan = minOf(dan, danToSphereInner(
                    point,
                    Vector3d(
                            (arena.width / 2) - arena.corner_radius,
                            point.y,
                            (arena.depth / 2) - arena.corner_radius
                    ),
                    arena.corner_radius
            ))
        }

        // Goal outer corner
        if (point.z < (arena.depth / 2) + arena.goal_side_radius) {
            // Side x
            if (point.x < (arena.goal_width / 2) + arena.goal_side_radius) {
                dan = minOf(dan, danToSphereOuter(
                        point,
                        Vector3d(
                                (arena.goal_width / 2) + arena.goal_side_radius,
                                point.y,
                                (arena.depth / 2) + arena.goal_side_radius
                        ),
                        arena.goal_side_radius
                ))
            }
            // Ceiling
            if (point.y < arena.goal_height + arena.goal_side_radius) {
                dan = minOf(dan, danToSphereOuter(
                        point,
                        Vector3d(
                                point.x,
                                arena.goal_height + arena.goal_side_radius,
                                (arena.depth / 2) + arena.goal_side_radius
                        ),
                        arena.goal_side_radius
                ))
            }
            //Top corner
            var topCornerPoint = Vector2d(
                    (arena.goal_width / 2) - arena.goal_top_radius,
                    arena.goal_height - arena.goal_top_radius
            )
            val diff = Vector2d(point.x, point.y) - topCornerPoint
            if (diff.x > 0 && diff.y > 0) {
                topCornerPoint += diff.normalize() * (arena.goal_top_radius + arena.goal_side_radius)
                dan = minOf(dan, danToSphereOuter(
                        point,
                        Vector3d(topCornerPoint.x, topCornerPoint.y, (arena.depth / 2) + arena.goal_side_radius),
                        arena.goal_side_radius
                ))
            }
        }

        // Goal inside top corners
        if (point.z > (arena.depth / 2) + arena.goal_side_radius
                && point.y > arena.goal_height - arena.goal_top_radius) {
            // Side x
            if (point.x > (arena.goal_width / 2) - arena.goal_top_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                (arena.goal_width / 2) - arena.goal_top_radius,
                                arena.goal_height - arena.goal_top_radius,
                                point.z
                        ),
                        arena.goal_top_radius
                ))
            }
            // Side z
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                point.x,
                                arena.goal_height - arena.goal_top_radius,
                                (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius
                        ),
                        arena.goal_top_radius
                ))
            }
        }

        // Bottom corners
        if (point.y < arena.bottom_radius) {
            // Side x
            if (point.x > (arena.width / 2) - arena.bottom_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                (arena.width / 2) - arena.bottom_radius,
                                arena.bottom_radius,
                                point.z
                        ),
                        arena.bottom_radius
                ))
            }
            // Side z
            if (point.z > (arena.depth / 2) - arena.bottom_radius
                    && point.x >= (arena.goal_width / 2) + arena.goal_side_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                point.x,
                                arena.bottom_radius,
                                (arena.depth / 2) - arena.bottom_radius
                        ),
                        arena.bottom_radius
                ))
            }
            // Side z (goal)
            if (point.z > (arena.depth / 2) + arena.goal_depth - arena.bottom_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                point.x,
                                arena.bottom_radius,
                                (arena.depth / 2) + arena.goal_depth - arena.bottom_radius
                        ),
                        arena.bottom_radius
                ))
            }
            // Goal outer corner
            var o = Vector2d(
                    (arena.goal_width / 2) + arena.goal_side_radius,
                    (arena.depth / 2) + arena.goal_side_radius
            )
            val v = Vector2d(point.x, point.z) - o
            if (v.x < 0 && v.y < 0 && v.length() < arena.goal_side_radius + arena.bottom_radius) {
                o += v.normalize() * (arena.goal_side_radius + arena.bottom_radius)
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(o.x, arena.bottom_radius, o.y),
                        arena.bottom_radius
                ))
            }
            // Side x (goal)
            if (point.z >= (arena.depth / 2) + arena.goal_side_radius && point.x > (arena.goal_width / 2) - arena.bottom_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                (arena.goal_width / 2) - arena.bottom_radius,
                                arena.bottom_radius,
                                point.z
                        ),
                        arena.bottom_radius
                ))
            }

            // Corner
            if (point.x > (arena.width / 2) - arena.corner_radius && point.z > (arena.depth / 2) - arena.corner_radius) {
                val cornerO = Vector2d(
                        (arena.width / 2) - arena.corner_radius,
                        (arena.depth / 2) - arena.corner_radius
                )
                var n = Vector2d(point.x, point.z) - cornerO
                val dist = n.length()
                if (dist > arena.corner_radius - arena.bottom_radius) {
                    n /= dist
                    val o2 = cornerO + n * (arena.corner_radius - arena.bottom_radius)
                    dan = minOf(dan, danToSphereInner(
                            point,
                            Vector3d(o2.x, arena.bottom_radius, o2.y),
                            arena.bottom_radius
                    ))
                }
            }
        }

        // Ceiling corners
        if (point.y > arena.height - arena.top_radius) {
            // Side x
            if (point.x > (arena.width / 2) - arena.top_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                (arena.width / 2) - arena.top_radius,
                                arena.height - arena.top_radius,
                                point.z
                        ),
                        arena.top_radius
                ))
            }
            // Side z
            if (point.z > (arena.depth / 2) - arena.top_radius) {
                dan = minOf(dan, danToSphereInner(
                        point,
                        Vector3d(
                                point.x,
                                arena.height - arena.top_radius,
                                (arena.depth / 2) - arena.top_radius
                        ),
                        arena.top_radius
                ))
            }
            // Corner
            if (point.x > (arena.width / 2) - arena.corner_radius && point.z > (arena.depth / 2) - arena.corner_radius) {
                val cornerO = Vector2d(
                        (arena.width / 2) - arena.corner_radius,
                        (arena.depth / 2) - arena.corner_radius
                )
                val dv = Vector2d(point.x, point.z) - cornerO
                if (dv.length() > arena.corner_radius - arena.top_radius) {
                    val n = dv.normalize()
                    val o2 = cornerO + n * (arena.corner_radius - arena.top_radius)
                    dan = minOf(dan, danToSphereInner(
                            point,
                            Vector3d(o2.x, arena.height - arena.top_radius, o2.y),
                            arena.top_radius
                    ))
                }
            }
        }

        return dan
    }

    private fun clamp(vector3d: Vector3d, maxValue: Double): Vector3d {
        val reductionCoefficient = vector3d.length() / maxValue
        return Vector3d(
                vector3d.x * reductionCoefficient,
                vector3d.y * reductionCoefficient,
                vector3d.z * reductionCoefficient
        )
    }

    data class Dan(
            val distance: Double,
            val normal: Vector3d) : Comparable<Dan> {
        override fun compareTo(other: Dan): Int {
            return distance.compareTo(other.distance)
        }
    }
}
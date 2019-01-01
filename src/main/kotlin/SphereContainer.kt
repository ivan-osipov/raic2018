import com.google.gson.annotations.SerializedName

class SphereContainer(
        @SerializedName("Sphere") val sphere: Sphere
): IDebugInfo

class Sphere(
        val x: Double,
        val y: Double,
        val z: Double,
        val radius: Double = 0.1,
        val r: Double = 1.0,
        val g: Double = 1.0,
        val b: Double = 1.0,
        val a: Double = 0.5
) {
    constructor(point: Vector3d,
                radius: Double = 1.0, r: Double = 1.0, g: Double = 1.0, b: Double = 1.0, a: Double = 1.0)
            : this(point.x, point.y, point.z, radius, r, g, b, a)
}
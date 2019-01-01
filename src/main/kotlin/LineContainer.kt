import com.google.gson.annotations.SerializedName

class LineContainer(
        @SerializedName("Line") val line: Line
): IDebugInfo

class Line(
        val x1: Double,
        val y1: Double,
        val z1: Double,
        val x2: Double,
        val y2: Double,
        val z2: Double,
        val width: Double = 1.0,
        val r: Double = 1.0,
        val g: Double = 1.0,
        val b: Double = 1.0,
        val a: Double = 1.0
) {

    constructor(from: Vector3d, to: Vector3d,
                radius: Double = 1.0, r: Double = 1.0, g: Double = 1.0, b: Double = 1.0, a: Double = 1.0)
            : this(from.x, from.y, from.z, to.x, to.y, to.z, radius, r, g, b, a)
}
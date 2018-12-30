import java.lang.String.format
import kotlin.math.max
import kotlin.math.min

open class Matrix<T>(val width: Int, val height: Int, init: (Int, Int) -> T) {
    private val content: List<MutableList<T>>

    init {
        content = (0 until width).mapTo(ArrayList()) { row ->
            (0 until height).mapTo(ArrayList()) { column -> init(row, column) }
        }
    }

    operator fun get(i: Int, j: Int): T {
        val effectiveX = max(0, min(i, width - 1))
        val effectiveY = max(0, min(j, height - 1))

        return content[effectiveX][effectiveY]
    }

    operator fun set(i: Int, j: Int, value: T) {
        content[i][j] = value
    }

    override fun toString() = buildString {
        for(i in 0 until height) {
            for(j in 0 until width) {
                append(format("%3.2f ", content[j][i]))
            }
            append("\n")
        }
    }
}
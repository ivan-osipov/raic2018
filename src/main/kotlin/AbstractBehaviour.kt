import model.Action
import model.Game
import model.Robot
import model.Rules

abstract class AbstractBehaviour {

    lateinit var me: Robot
    lateinit var rules: Rules
    lateinit var game: Game
    lateinit var action: Action

    protected abstract fun perform()

    public fun doIt() {
        perform()
    }

    fun debug(): List<IDebugInfo> = emptyList()
}
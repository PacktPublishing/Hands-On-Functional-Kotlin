import arrow.effects.IO

interface State

interface DataSource {
    suspend fun downloadNames(): IO<List<String>>
}

//Immutable app state
data class RootState(
        val title: String = "Hands On Functional Kotlin",
        val refreshText: String = "Refresh",
        val clearText: String = "Clear",
        val loadingText: String = "Loading...",
        val errorText: String = "Error refreshing, try again",
        val showLoading: Boolean = true,
        val showError: Boolean = false,
        val names: List<String> = emptyList()
) : State

interface Binder<T : State> {
    fun bind(state: T): T
}

sealed class Action
object Refresh : Action()
object Clear : Action()

interface ActionHandler<out R : Any> {
    fun dispatchAction(a: Action): R
}
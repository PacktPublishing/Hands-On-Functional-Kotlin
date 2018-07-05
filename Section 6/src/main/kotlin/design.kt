import arrow.effects.IO

//UI
interface Binder<T : State> {
    fun bind(state: T): T
}

interface State

//Action
sealed class Action

object Refresh : Action()
object Clear : Action()

//Reducer
// (State, Action) -> State

//Store
interface Store<S : State> {
    val state: S
    fun push(state: S)
    fun observe(callback: (S) -> Unit)
}

//Handling Async Actions
interface DataSource {
    fun downloadNames(): IO<List<String>>
}

interface MiddleWare<in S : State> {
    fun handleAction(state: S, action: Action)
}

import arrow.effects.IO
import arrow.syntax.function.curried
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.util.Random
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

class DataSourceImpl : DataSource {
    override suspend fun downloadNames(): IO<List<String>> {
        delay(2000)
        return IO {
            if (Random().nextInt() % 2 == 0) throw IOException()
            listOf("Adam", "Alex", "Alfred", "Albert",
                    "Brenda", "Connie", "Derek", "Donny",
                    "Lynne", "Myrtle", "Rose", "Rudolph",
                    "Tony", "Trudy", "Williams", "Zach"
            ).shuffled()
        }
    }
}

//Side effects container
class ActionHandlerImpl(
        val dataSource: DataSource,
        binderProducer: () -> Binder<RootState>,
        var state: RootState
) : ActionHandler<Job> {

    val binder by lazy(binderProducer)

    override fun dispatchAction(a: Action): Job = launch(UI) {
        state = reduce(
                a,
                { refresh(state, binder, dataSource) },
                { clear(state, binder) }
        )
    }

}

inline fun reduce(a: Action, refresh: () -> RootState, clear: () -> RootState): RootState =
        when (a) {
            Refresh -> refresh()
            Clear -> clear()
        }

val errorHandler = { state: RootState, binder: Binder<RootState>, _: Throwable ->
    updateState(state, Unit, binder) {
        copy(showLoading = false, showError = true)
    }
}.curried()

val refreshHandler = { state: RootState, binder: Binder<RootState>, names: List<String> ->
    updateState(state, names, binder) {
        copy(names = it, showLoading = false, showError = false)
    }
}.curried()

private suspend fun clear(state: RootState, binder: Binder<RootState>) =
        updateState(state, emptyList<String>(), binder) { copy(names = it) }

private suspend fun refresh(state: RootState, binder: Binder<RootState>, dataSource: DataSource): RootState {
    updateState(state, Unit, binder) { copy(showLoading = true, showError = false) }
    return dataSource.downloadNames().await().fold(
            errorHandler(state)(binder),
            refreshHandler(state)(binder)
    )
}

inline fun <T : Any> updateState(
        state: RootState,
        arg: T,
        binder: Binder<RootState>,
        f: RootState.(T) -> RootState
): RootState = f(state, arg).let(binder::bind)

class RootView(val primaryStage: Stage) {

    val width = 480.0
    val height = 640.0
    val refresh = Button()
    val clear = Button()
    val loading = Text()
    val error = Text()
    val list = ListView<String>()

    init {

        list.setPrefSize(width, height)

        val root = VBox()

        val buttons = HBox()
        buttons.children.add(refresh)
        buttons.children.add(clear)
        root.children.add(buttons)
        val messages = HBox(loading, error)
        HBox.setMargin(loading, Insets(16.0))
        HBox.setMargin(error, Insets(16.0))
        root.children.add(messages)
        root.children.add(list)
        primaryStage.scene = Scene(root, width, height)
        primaryStage.show()
    }
}

//Side effects container
class BinderImpl(private val view: RootView, val actionHandler: ActionHandler<Job>) : Binder<RootState> {

    val data: ObservableList<String> = FXCollections.observableArrayList()

    override fun bind(state: RootState): RootState = state.apply {
        data.apply {
            clear()
            addAll(names)
        }
        view.apply {
            primaryStage.title = title
            loading.text = loadingText
            refresh.text = refreshText
            clear.text = clearText
            list.items = data
            error.text = errorText
            loading.isVisible = showLoading
            error.isVisible = showError
            refresh.setOnMouseClicked { actionHandler.dispatchAction(Refresh) }
            clear.setOnMouseClicked { actionHandler.dispatchAction(Clear) }
        }
    }
}
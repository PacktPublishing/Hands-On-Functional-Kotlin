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
import javafx.scene.transform.Transform
import javafx.stage.Stage
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import java.io.IOException
import java.util.Random
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

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
        root.transforms.add(Transform.scale(2.0,2.0))
        VBox.setMargin(root, Insets(240.0))
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

//Side effects container
class BinderImpl(
        private val view: RootView,
        private val actionHandler: (Action) -> Unit
) : Binder<RootState> {

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
            refresh.setOnMouseClicked { actionHandler(Refresh) }
            clear.setOnMouseClicked { actionHandler(Clear) }
        }
    }
}

class DataSourceImpl : DataSource {
    override fun downloadNames(): IO<List<String>> = IO {
        Thread.sleep(2000)
        if (Random().nextInt() % 2 == 0) throw IOException()
        listOf("Adam", "Alex", "Alfred", "Albert",
                "Brenda", "Connie", "Derek", "Donny",
                "Lynne", "Myrtle", "Rose", "Rudolph",
                "Tony", "Trudy", "Williams", "Zach"
        ).shuffled()
    }
}

class StoreImpl : Store<RootState> {
    override var state: RootState = RootState()
    var observers: Set<(RootState) -> Unit> = emptySet()

    override fun push(state: RootState) {
        this.state = state
        observers.forEach { it(state) }
    }

    override fun observe(callback: (RootState) -> Unit) {
        observers += callback
    }

}

class MiddleWareImpl(
        val dataSource: () -> IO<List<String>>,
        val callback: (RootState) -> Unit
) : MiddleWare<RootState> {
    override fun handleAction(state: RootState, action: Action) {
        launch(UI) {
            if (action == Refresh) {
                val result = withContext(CommonPool) {
                    dataSource().await()
                }
                result.fold(errorHandler(state), refreshHandler(state))
                        .let(callback)
            }
        }
    }
}

val errorHandler = { state: RootState, _: Throwable ->
    updateState(state, Unit) {
        copy(showLoading = false, showError = true)
    }
}.curried()

val refreshHandler = { state: RootState, names: List<String> ->
    updateState(state, names) {
        copy(names = it, showLoading = false, showError = false)
    }
}.curried()

fun clear(state: RootState) =
        updateState(state, emptyList<String>()) { copy(names = it) }

fun refresh(state: RootState): RootState =
        updateState(state, Unit) { copy(showLoading = true, showError = false) }

inline fun <T : Any> updateState(
        state: RootState,
        arg: T,
        f: RootState.(T) -> RootState
): RootState = f(state, arg)
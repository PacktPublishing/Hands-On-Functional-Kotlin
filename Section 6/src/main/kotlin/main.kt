import javafx.application.Application
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
import kotlinx.coroutines.experimental.launch as coroutinesLaunch
import kotlinx.coroutines.experimental.delay
import java.io.IOException
import java.util.Random
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

class FunctionalKotlinSample : Application() {

    lateinit var binder: Binder

    override fun start(primaryStage: Stage) {
        val dataSource = DataSource()
        val state = RootState()
        val dispatcher = Dispatcher(dataSource, { binder }, state)
        val rootView = RootView(primaryStage)
        binder = Binder(rootView, dispatcher)
        dispatcher.dispatchAction(Refresh)
    }

    companion object {
        // Entry point
        @JvmStatic fun main(args: Array<String>) {
            // Launch example App
            launch(FunctionalKotlinSample::class.java)
        }
    }
}

class DataSource {
    suspend fun downloadNames(): List<String> {
        delay(2000)
        if (Random().nextInt() % 5 == 0) throw IOException()
        return listOf("Adam", "Alex", "Alfred", "Albert",
                "Brenda", "Connie", "Derek", "Donny",
                "Lynne", "Myrtle", "Rose", "Rudolph",
                "Tony", "Trudy", "Williams", "Zach"
        ).shuffled()
    }
}

sealed class Action
object Refresh : Action()
object Clear : Action()

class Dispatcher(val dataSource: DataSource, val binder: () -> Binder, var state: RootState) {
    fun dispatchAction(a: Action) = when (a) {
        Refresh -> refresh()
        Clear -> clear()
    }

    private fun clear() = coroutinesLaunch(UI) {
        state = updateState(state, emptyList<String>(), binder) { copy(names = it) }
    }

    private fun refresh() = coroutinesLaunch(UI) {
        state = updateState(state, Unit, binder) { copy(showLoading = true) }
        val names = dataSource.downloadNames()
        state = updateState(state, names, binder) { copy(names = it, showLoading = false) }
    }

    private fun <T : Any> updateState(state: RootState, arg: T, binder: () -> Binder, f: RootState.(T) -> RootState): RootState =
            f(state, arg).let(binder()::bind)

}

//Immutable app state
data class RootState(
        val title: String = "Hands On Functional Kotlin",
        val refreshText: String = "Refresh",
        val clearText: String = "Clear",
        val loadingText: String = "Loading...",
        val showLoading: Boolean = true,
        val names: List<String> = emptyList())

class RootView(val primaryStage: Stage) {

    val width = 480.0
    val height = 640.0
    val refresh = Button()
    val clear = Button()
    val loading = Text()
    val list = ListView<String>()

    init {

        list.setPrefSize(width, height)

        val root = VBox()

        VBox.setMargin(loading, Insets(16.0))
        val buttons = HBox()
        buttons.children.add(refresh)
        buttons.children.add(clear)
        root.children.add(buttons)
        root.children.add(loading)
        root.children.add(list)
        primaryStage.scene = Scene(root, width, height)
        primaryStage.show()
    }
}

class Binder(private val view: RootView, val dispatcher: Dispatcher) {

    val data: ObservableList<String> = FXCollections.observableArrayList()

    fun bind(state: RootState): RootState = state.apply {
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
            loading.isVisible = showLoading
            refresh.setOnMouseClicked { dispatcher.dispatchAction(Refresh) }
            clear.setOnMouseClicked { dispatcher.dispatchAction(Clear) }
        }
    }
}
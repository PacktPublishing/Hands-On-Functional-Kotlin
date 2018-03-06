import javafx.application.Application
import javafx.stage.Stage

class FunctionalKotlinSample : Application() {

    lateinit var binder: Binder<RootState>

    override fun start(primaryStage: Stage) {
        val dataSource = DataSourceImpl()
        val state = RootState()
        val dispatcher = ActionHandlerImpl(dataSource, { binder }, state)
        val rootView = RootView(primaryStage)
        binder = BinderImpl(rootView, dispatcher)
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
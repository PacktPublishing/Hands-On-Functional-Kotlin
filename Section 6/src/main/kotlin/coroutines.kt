import arrow.core.Either
import arrow.effects.IO
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.suspendCoroutine
//Coroutine dispatcher
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

suspend fun <T : Any> IO<T>.await(): Either<Throwable, T> =
        suspendCoroutine { continuation ->
            unsafeRunAsync(continuation::resume)
        }

fun main(args: Array<String>) {
    runBlocking {
        IO { "Hello Coroutine" }
                // Run asynchronously
                .await()
                // Wait for result on Coroutine dispatcher
                .fold(
                        { println("Error $it")},
                        { println("Success $it")}
                )
    }

    launch(UI) {  }
}
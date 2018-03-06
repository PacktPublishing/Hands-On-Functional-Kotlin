import arrow.core.Either
import arrow.effects.IO
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun <T : Any> IO<T>.await(): Either<Throwable, T> =
        suspendCoroutine { continuation ->
            unsafeRunAsync(continuation::resume)
        }

fun main(args: Array<String>) = runBlocking {
    IO { "Hello Coroutine" }
            .await()
            .fold(
                    { println("Error $it")},
                    { println("Success $it")}
            )
}
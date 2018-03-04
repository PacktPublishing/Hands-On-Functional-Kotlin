import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val job = async {
        delay(1000)
        "Hello"
    }

    val job2 = async {
        delay(1000)
        "World"
    }

    runBlocking {
        val text = "${job.await()} ${job2.await()}"
        print(text)
    }

}
import arrow.core.Either
import arrow.core.Option
import arrow.core.Some
import arrow.core.ev
import arrow.core.monad
import arrow.effects.IO
import arrow.effects.ev
import arrow.effects.milliseconds
import arrow.effects.monad
import arrow.syntax.either.left
import arrow.syntax.either.right
import arrow.typeclasses.binding

class DataBase {
    fun readUserName() = "John"
    fun readAnotherUserName() = "Howard"
}

val dataBase: DataBase = DataBase()

fun main(args: Array<String>) {
    IO.pure(43)
    IO.raiseError<Int>(RuntimeException())
    IO.invoke { dataBase.readUserName() }
    IO { "Hello IO" }
    IO.async { result: (Either<Throwable, String>) -> Unit ->
        try {
            result(dataBase.readUserName().right())
        } catch (e: Throwable) {
            result(e.left())
        }
    }

    ioSync()
    ioRunAsync()
    ioAttempt()
    ioBinding()

}

fun ioSync() {
    //infinite timeout
    val username: String = IO { dataBase.readUserName() }
            .unsafeRunSync()

    val optionUser = IO { dataBase.readUserName() }
            .unsafeRunTimed(100.milliseconds)
    print(username)
    when (optionUser) {
        is Some -> print(optionUser.t)
    }
}

fun ioRunAsync() {
    IO { dataBase.readUserName() }
            //unsafeRunAsync
            .runAsync {
                it.fold(
                        { IO { print("Error ${it.localizedMessage}") } },
                        { IO { print("Success $it") } }
                )
            }
            .unsafeRunSync()
}

fun ioAttempt() {
    IO { dataBase.readUserName() }
            .attempt()
            .runAsync {
                it.fold(
                        { IO { print("Async error ${it.localizedMessage}") } },
                        {
                            it.fold(
                                    { IO { print("IO Error ${it.localizedMessage}") } },
                                    { IO { print("Success: $it") } }
                            )
                        }
                )
            }.unsafeRunSync()
}

fun ioBinding() {
    // Parallel
    IO.monad().binding {
        val a = IO { dataBase.readUserName() }.bind() //Suspend coroutine
        val b = IO { dataBase.readAnotherUserName() }.bind()
        "$a $b"
    }.ev()// Down casting result of binding fun to IO
            .unsafeRunSync().let(::print)

    println()

    Option.monad().binding {
        //...
        val a = IO { dataBase.readUserName() }
                .unsafeRunTimed(100.milliseconds).bind()
        val b = Option(43).bind()
        a + b
    }.ev().let(::print)
}

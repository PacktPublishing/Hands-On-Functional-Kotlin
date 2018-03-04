import arrow.core.value
import arrow.data.Reader
import arrow.data.andThen
import arrow.syntax.foldable.fold

fun main(args: Array<String>) {
    dbReader()
    dbReaderLocal()
    dbReaderAndThen()
}

fun dbReader() {
    val dataBase = DataBase()
    Reader { db: DataBase -> db.readUserName() }
            .run(dataBase)
            .value()
            .let(::print)
}

fun dbReaderLocal() {
    val dataBase1 = DataBase()
    val dataBase2 = DataBase()
    val dbCall = Reader { db: DataBase -> db.readUserName() }
            //Changed signature from A -> B to C -> B
            .local<Int> {
                when (it) {
                    1 -> dataBase1
                    else -> dataBase2
                }
            }
    dbCall.run(1)
            .value()
            .let(::print)
}

fun dbReaderAndThen() {
    val dataBase = DataBase()
    fun newReader(s: String) = when (s) {
        "John" -> Reader { it: String -> +it.length }
        else -> Reader { it: String -> +it.length * 2 }
    }
    Reader { db: DataBase -> db.readUserName() }
            .andThen(::newReader)
            .run(dataBase)
            .fold()
            .let(::print)
}

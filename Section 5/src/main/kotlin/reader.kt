import arrow.core.ev
import arrow.core.value
import arrow.data.Reader
import arrow.data.andThen

fun main(args: Array<String>) {
    dbReader()
    dbReaderLocal()
    dbReaderAndThen()
}

fun dbReader() {
    val dataBase = DataBase()
    Reader { db: DataBase -> db.readUserName() }
            .run.invoke(dataBase)
            .value()
            .let(::println)
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
    dbCall.run.invoke(1)
            .value()
            .let(::println)
}

fun dbReaderAndThen() {
    val dataBase = DataBase()
    fun newReader(s: String) = when (s) {
        "John" -> s.length
        else -> s.length * 2
    }
    Reader { db: DataBase -> db.readUserName() }
            .andThen(::newReader)
            .run(dataBase)
            .value()
            .let(::println)
}

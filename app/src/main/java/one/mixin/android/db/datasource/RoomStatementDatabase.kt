package one.mixin.android.db.datasource

import androidx.room3.RoomDatabase

class RoomStatementDatabase internal constructor(
    private val db: RoomDatabase,
) {
    private var transactionStatements: MutableList<Pair<String, Array<Any?>>>? = null
    private var transactionSuccessful = false

    fun compileStatement(sql: String): RoomStatement = RoomStatement(this, sql)

    fun beginTransaction() {
        transactionStatements = mutableListOf()
        transactionSuccessful = false
    }

    fun setTransactionSuccessful() {
        transactionSuccessful = true
    }

    fun endTransaction() {
        val statements = transactionStatements
        transactionStatements = null
        if (transactionSuccessful && statements != null) {
            RoomDatabaseCompat.runInWriteTransaction(db) {
                statements.forEach { (sql, bindArgs) ->
                    execSQL(sql, bindArgs)
                }
            }
        }
        transactionSuccessful = false
    }

    internal fun execute(
        sql: String,
        bindArgs: Array<Any?>,
    ) {
        val statements = transactionStatements
        if (statements == null) {
            RoomDatabaseCompat.execute(db, sql, bindArgs)
        } else {
            statements += sql to bindArgs
        }
    }
}

class RoomStatement internal constructor(
    private val db: RoomStatementDatabase,
    private val sql: String,
) {
    private val bindArgs = linkedMapOf<Int, Any?>()

    fun bindNull(index: Int) {
        bindArgs[index] = null
    }

    fun bindLong(
        index: Int,
        value: Long,
    ) {
        bindArgs[index] = value
    }

    fun bindLong(
        index: Int,
        value: Int,
    ) {
        bindArgs[index] = value.toLong()
    }

    fun bindString(
        index: Int,
        value: String,
    ) {
        bindArgs[index] = value
    }

    fun bindBlob(
        index: Int,
        value: ByteArray,
    ) {
        bindArgs[index] = value
    }

    fun executeInsert(): Long {
        val args = Array((bindArgs.keys.maxOrNull() ?: 0)) { index -> bindArgs[index + 1] }
        db.execute(sql, args)
        bindArgs.clear()
        return -1L
    }

    fun close() = Unit
}

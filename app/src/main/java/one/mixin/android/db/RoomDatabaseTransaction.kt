package one.mixin.android.db

import androidx.room3.RoomDatabase
import androidx.room3.util.performInTransactionSuspending
import one.mixin.android.db.datasource.RoomDatabaseCompat

fun <T> RoomDatabase.runInTransaction(block: () -> T): T = RoomDatabaseCompat.runInWriteTransaction(this) { block() }

suspend fun <T> RoomDatabase.withRoomTransaction(block: suspend () -> T): T =
    performInTransactionSuspending(this, block)

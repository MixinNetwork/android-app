package one.mixin.android.db

import androidx.room3.RoomDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat

fun <T> RoomDatabase.runInTransaction(block: () -> T): T = RoomDatabaseCompat.runInWriteTransaction(this) { block() }

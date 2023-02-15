package one.mixin.android.fts5

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

open class SqlHelper(context: Context?, name: String, version: Int, private val createSql: List<String>) :
    SQLiteOpenHelper(context, name, null, version) {
    override fun onCreate(db: SQLiteDatabase) {
        createSql.forEach {
            db.execSQL(it)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when {
            oldVersion == 1 -> {
                // Todo
            }
            else -> {

            }
        }
    }
}
package one.mixin.android.db.datasource

import androidx.room3.RoomRawQuery
import androidx.sqlite.SQLiteStatement

private val roomRawQueryBindingFunctionMethod by lazy {
    RoomRawQuery::class.java.getDeclaredMethod("getBindingFunction")
}

internal object RoomRawQueryCompat {
    @Suppress("UNCHECKED_CAST")
    fun bind(
        query: RoomRawQuery,
        statement: SQLiteStatement,
    ) {
        val bindingFunction = roomRawQueryBindingFunctionMethod.invoke(query) as (SQLiteStatement) -> Unit
        bindingFunction(statement)
    }
}

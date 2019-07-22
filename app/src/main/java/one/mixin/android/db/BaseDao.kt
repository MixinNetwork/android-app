package one.mixin.android.db

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg obj: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(obj: List<T>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg obj: T)

    @Delete
    fun delete(vararg obj: T)

    @Delete
    fun deleteList(obj: List<T>)

    companion object {
        const val ESCAPE_SUFFIX = " ESCAPE '\\'"
    }
}

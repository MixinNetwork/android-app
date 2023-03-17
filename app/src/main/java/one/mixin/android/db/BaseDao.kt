package one.mixin.android.db

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Upsert

interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspend(vararg obj: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListSuspend(obj: List<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg obj: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReturn(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(obj: List<T>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg obj: T)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateList(obj: List<T>)

    @Delete
    fun delete(vararg obj: T)

    @Delete
    fun deleteList(obj: List<T>)

    @Upsert
    suspend fun upsert(entity: T)

    @Upsert
    suspend fun upsertList(obj: List<T>)

    companion object {
        const val ESCAPE_SUFFIX = " ESCAPE '\\'"
    }
}

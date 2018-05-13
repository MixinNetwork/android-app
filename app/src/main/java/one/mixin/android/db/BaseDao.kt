package one.mixin.android.db

import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Update

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
}
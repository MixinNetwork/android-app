package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Property

@Dao
interface PropertyDao : BaseDao<Property> {

    @Query("SELECT `key` FROM Properties WHERE `key` = :key")
    suspend fun get(key: String): String?
}
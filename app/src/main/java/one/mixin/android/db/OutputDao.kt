package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT created_at FROM outputs ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestOutputCreatedAt(): String?
}
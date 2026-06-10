package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.FavoriteApp

@Dao
interface FavoriteAppDao : BaseDao<FavoriteApp> {
    @Query("DELETE FROM favorite_apps WHERE app_id =:appId AND user_id=:userId")
    suspend fun deleteByAppIdAndUserId(
        appId: String,
        userId: String,
    )

    @Query("DELETE FROM favorite_apps WHERE user_id=:userId")
    suspend fun deleteByUserId(userId: String)
}

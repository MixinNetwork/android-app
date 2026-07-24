package one.mixin.android.db

import androidx.room3.Dao
import androidx.room3.Query
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

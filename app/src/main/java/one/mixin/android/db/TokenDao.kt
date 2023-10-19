package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Token

@Dao
interface TokenDao : BaseDao<Token> {

    @Query("SELECT asset_id FROM tokens WHERE asset = :asset")
    suspend fun checkExists(asset: String): String?

    @Query("SELECT * FROM tokens WHERE asset = :asset")
    suspend fun findTokenByAsset(asset: String): Token?
}

package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import one.mixin.android.vo.safe.TokensExtra

@Dao
interface TokensExtraDao : BaseDao<TokensExtra> {
    @Query("SELECT * FROM tokens_extra WHERE asset_id = :assetId")
    suspend fun findByAssetId(assetId: String): TokensExtra?

    @Query("SELECT * FROM tokens_extra WHERE kernel_asset_id = :asset")
    suspend fun findByAsset(asset: String): TokensExtra?

    @Query("UPDATE tokens_extra SET hidden = :hidden WHERE asset_id = :assetId")
    suspend fun updateHiddenByAssetId(
        assetId: String,
        hidden: Boolean,
    )

    @Query("UPDATE tokens_extra SET balance = :balance, updated_at = :updatedAt WHERE asset_id = :assetId")
    suspend fun updateBalanceByAssetId(
        assetId: String,
        balance: String,
        updatedAt: String,
    )

    @Query("SELECT * FROM tokens_extra WHERE asset_id = :assetId")
    fun tokenExtraFlow(assetId: String): Flow<TokensExtra?>
}

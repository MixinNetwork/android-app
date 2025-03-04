package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3TokensExtra

@Dao
interface Web3TokensExtraDao : BaseDao<Web3TokensExtra> {
    @Query("SELECT * FROM web3_tokens_extra WHERE asset_id = :assetId")
    suspend fun findByAssetId(assetId: String): Web3TokensExtra?

    @Query("UPDATE web3_tokens_extra SET hidden = :hidden, updated_at = :updatedAt WHERE asset_id = :assetId")
    suspend fun updateHidden(assetId: String, hidden: Boolean, updatedAt: String)
}

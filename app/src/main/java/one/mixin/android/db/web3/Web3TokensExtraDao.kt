package one.mixin.android.db.web3

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.db.BaseDao
import one.mixin.android.db.web3.vo.Web3TokensExtra

@Dao
interface Web3TokensExtraDao : BaseDao<Web3TokensExtra> {
    @Query("SELECT * FROM tokens_extra WHERE asset_id = :assetId AND wallet_id = :walletId")
    suspend fun findByAssetId(assetId: String, walletId: String): Web3TokensExtra?

    @Query("UPDATE tokens_extra SET hidden = :hidden WHERE asset_id = :assetId AND wallet_id = :walletId")
    suspend fun updateHidden(assetId: String, walletId: String, hidden: Boolean)
}

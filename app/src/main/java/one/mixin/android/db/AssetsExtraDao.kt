package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.AssetsExtra

@Dao
interface AssetsExtraDao : BaseDao<AssetsExtra> {
    @Query("SELECT * FROM assets_extra WHERE asset_id = :assetId")
    suspend fun findByAssetId(assetId: String): AssetsExtra?

    @Query("UPDATE assets_extra SET hidden=:hidden WHERE asset_id=:assetId")
    suspend fun updateHiddenByAssetId(assetId: String, hidden: Boolean)

    @Query("UPDATE assets_extra SET balance=:balance, utxo_id=:utxoId WHERE asset_id=:assetId")
    suspend fun updateBalanceAndUtxoIdByAssetId(assetId: String, balance: String, utxoId: String)
}

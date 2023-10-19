package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.AssetsExtra

@Dao
interface AssetsExtraDao : BaseDao<AssetsExtra> {
    @Query("SELECT * FROM assets_extra WHERE asset_id = :assetId")
    suspend fun findByAssetId(assetId: String): AssetsExtra?

    @Query("SELECT * FROM assets_extra WHERE asset = :asset")
    suspend fun findByAsset(asset: String): AssetsExtra?

    @Query("UPDATE assets_extra SET hidden=:hidden WHERE asset_id = :assetId")
    suspend fun updateHiddenByAssetId(assetId: String, hidden: Boolean)

    @Query("UPDATE assets_extra SET balance = :balance, updated_at = :updatedAt WHERE asset_id = :assetId")
    suspend fun updateBalanceByAssetId(assetId: String, balance: String, updatedAt: String)
}

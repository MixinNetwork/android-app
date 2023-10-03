package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT created_at FROM outputs ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestOutputCreatedAt(): String?

    @Query("SELECT utxo_id FROM outputs WHERE asset_id = :assetId ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestUtxoIdByAssetId(assetId: String): String?

    @Query("SELECT sum(amount) FROM outputs WHERE asset_id =:assetId AND state = 'unspent' AND created_at > (SELECT created_at FROM outputs WHERE utxo_id =:utxoId)")
    suspend fun calcAmountByUtxoIdAndAssetId(assetId: String, utxoId: String): Double

    @Query("SELECT sum(amount) FROM outputs WHERE asset_id =:assetId AND state = 'unspent'")
    suspend fun calcAmountByAssetId(assetId: String): Double
}

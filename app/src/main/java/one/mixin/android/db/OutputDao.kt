package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.job.UtxoProcessor.Companion.processUtxoLimit
import one.mixin.android.vo.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT * FROM outputs WHERE state = 'unspent' ORDER BY created_at ASC LIMIT :limit")
    suspend fun findOutputs(limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND created_at > (SELECT created_at FROM outputs WHERE output_id =:utxoId) ORDER BY created_at ASC LIMIT :limit")
    suspend fun findOutputsByUtxoId(utxoId: String, limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT created_at FROM outputs ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestOutputCreatedAt(): String?

    @Query("SELECT sum(amount) FROM outputs WHERE asset =:assetId AND state = 'unspent'")
    suspend fun calcBalanceByAssetId(assetId: String): Double
}

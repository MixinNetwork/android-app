package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.job.UtxoProcessor.Companion.processUtxoLimit
import one.mixin.android.vo.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT * FROM outputs WHERE state = 'unspent' ORDER BY created_at ASC LIMIT :limit")
    suspend fun findOutputs(limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' ORDER BY amount ASC LIMIT :limit")
    suspend fun findUnspentOutputsSortedByAmount(limit: Int): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY amount ASC LIMIT :limit")
    suspend fun findUnspentOutputsByAsset(limit: Int, asset: String): List<Output>

    @Query("SELECT output_id FROM outputs WHERE transaction_hash IN (:hash)")
    suspend fun findUtxoIds(hash: List<String>): List<String>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND created_at > (SELECT created_at FROM outputs WHERE output_id =:utxoId) ORDER BY created_at ASC LIMIT :limit")
    suspend fun findOutputsByUtxoId(utxoId: String, limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT created_at FROM outputs ORDER BY created_at DESC LIMIT 1")
    suspend fun findLatestOutputCreatedAt(): String?

    @Query("SELECT CAST(amount AS REAL) FROM outputs WHERE asset =:mixinId AND state = 'unspent'")
    suspend fun calcBalanceByAssetId(mixinId: String): Double

    @Query("SELECT DISTINCT asset FROM outputs")
    fun getMixinId(): List<String>

    @Query("UPDATE outputs SET state = 'signed' WHERE state = 'unspent' AND transaction_hash IN (:hash)")
    suspend fun updateUtxo(hash: List<String>)
}

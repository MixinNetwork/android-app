package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.job.UtxoProcessor.Companion.processUtxoLimit
import one.mixin.android.vo.safe.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT * FROM outputs WHERE state = 'unspent' ORDER BY sequence ASC LIMIT :limit")
    suspend fun findOutputs(limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY amount ASC LIMIT :limit")
    suspend fun findUnspentOutputsByAsset(limit: Int, asset: String): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY amount ASC LIMIT :limit OFFSET :offset")
    suspend fun findUnspentOutputsByAssetOffset(limit: Int, asset: String, offset: Int): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND sequence > (SELECT sequence FROM outputs WHERE output_id =:utxoId) ORDER BY sequence ASC LIMIT :limit")
    suspend fun findOutputsByUtxoId(utxoId: String, limit: Int = processUtxoLimit): List<Output>

    @Query("SELECT sequence FROM outputs ORDER BY sequence DESC LIMIT 1")
    suspend fun findLatestOutputSequence(): Long?

    @Query("SELECT CAST(amount AS REAL) FROM outputs WHERE asset =:mixinId AND state = 'unspent'")
    suspend fun calcBalanceByAssetId(mixinId: String): Double

    @Query("SELECT DISTINCT asset FROM outputs")
    fun getMixinId(): List<String>

    @Query("UPDATE outputs SET state = 'signed' WHERE state = 'unspent' AND output_id IN (:outputIds)")
    fun updateUtxoToSigned(outputIds: List<String>)
}

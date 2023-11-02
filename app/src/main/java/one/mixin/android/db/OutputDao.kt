package one.mixin.android.db

import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.safe.Output

@Dao
interface OutputDao : BaseDao<Output> {

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY amount ASC LIMIT :limit")
    suspend fun findUnspentOutputsByAsset(limit: Int, asset: String): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY amount ASC LIMIT :limit OFFSET :offset")
    suspend fun findUnspentOutputsByAssetOffset(limit: Int, asset: String, offset: Int): List<Output>

    @Query("SELECT sequence FROM outputs ORDER BY sequence DESC LIMIT 1")
    suspend fun findLatestOutputSequence(): Long?

    @Query("UPDATE outputs SET state = 'signed' WHERE state = 'unspent' AND output_id IN (:outputIds)")
    fun updateUtxoToSigned(outputIds: List<String>)
}

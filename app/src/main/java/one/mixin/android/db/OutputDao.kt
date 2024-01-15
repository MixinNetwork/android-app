package one.mixin.android.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.UtxoItem
import one.mixin.android.vo.safe.Output

@Dao
interface OutputDao : BaseDao<Output> {
    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY sequence ASC LIMIT :limit")
    suspend fun findUnspentOutputsByAsset(
        limit: Int,
        asset: String,
    ): List<Output>

    @Query("SELECT * FROM outputs WHERE state = 'unspent' AND asset = :asset ORDER BY sequence ASC LIMIT :limit OFFSET :offset")
    suspend fun findUnspentOutputsByAssetOffset(
        limit: Int,
        asset: String,
        offset: Int,
    ): List<Output>

    @Query("SELECT output_id FROM outputs WHERE output_id IN (:ids) AND state = 'signed'")
    fun findSignedOutput(ids: List<String>): List<String>

    @Query("SELECT sequence FROM outputs ORDER BY sequence DESC LIMIT 1")
    suspend fun findLatestOutputSequence(): Long?

    @Query("UPDATE outputs SET state = 'signed' WHERE state = 'unspent' AND output_id IN (:outputIds)")
    fun updateUtxoToSigned(outputIds: List<String>)

    @Query("SELECT * FROM outputs WHERE asset = :asset ORDER BY created_at DESC, rowid DESC")
    fun utxoItem(asset: String): PagingSource<Int, UtxoItem>

    @Query("DELETE FROM outputs WHERE asset = :asset AND sequence >= :offset")
    suspend fun deleteByKernelAssetIdAndOffset(
        asset: String,
        offset: Long,
    )
}

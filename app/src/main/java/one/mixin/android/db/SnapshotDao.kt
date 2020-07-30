package one.mixin.android.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Dao
interface SnapshotDao : BaseDao<Snapshot> {
    companion object {
        const val SNAPSHOT_ITEM_PREFIX =
            """
                SELECT s.*, u.avatar_url, u.full_name AS opponent_ful_name, a.symbol AS asset_symbol, a.confirmations AS asset_confirmations FROM snapshots s 
                LEFT JOIN users u ON u.user_id = s.opponent_id 
                LEFT JOIN assets a ON a.asset_id = s.asset_id 
            """
    }

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): DataSource.Factory<Int, SnapshotItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.amount * a.price_usd DESC, s.snapshot_id DESC")
    fun snapshotsOrderByAmount(assetId: String): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByType(assetId: String, type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY s.amount * a.price_usd DESC, s.snapshot_id DESC")
    fun snapshotsByTypeOrderByAmount(assetId: String, type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId and snapshot_id = :snapshotId")
    suspend fun snapshotLocal(assetId: String, snapshotId: String): SnapshotItem?

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE snapshot_id = :snapshotId")
    suspend fun findSnapshotById(snapshotId: String): SnapshotItem?

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE trace_id = :traceId")
    suspend fun findSnapshotByTraceId(traceId: String): SnapshotItem?

    @Query("$SNAPSHOT_ITEM_PREFIX ORDER BY s.created_at DESC")
    fun allSnapshots(): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX ORDER BY s.amount * a.price_usd DESC")
    fun allSnapshotsOrderByAmount(): DataSource.Factory<Int, SnapshotItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.type IN (:type, :otherType) ORDER BY s.created_at DESC")
    fun allSnapshotsByType(type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.type IN (:type, :otherType) ORDER BY s.amount * a.price_usd DESC")
    fun allSnapshotsByTypeOrderByAmount(type: String, otherType: String? = null): DataSource.Factory<Int, SnapshotItem>

    @RewriteQueriesToDropUnusedColumns
    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.opponent_id = :opponentId AND s.type != 'pending' ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByUserId(opponentId: String): DataSource.Factory<Int, SnapshotItem>

    @Query("DELETE FROM snapshots WHERE asset_id = :assetId AND type = 'pending'")
    suspend fun clearPendingDepositsByAssetId(assetId: String)

    @Query("DELETE FROM snapshots WHERE type = 'pending' AND transaction_hash = :transactionHash")
    fun deletePendingSnapshotByHash(transactionHash: String)
}

package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Dao
interface SnapshotDao : BaseDao<Snapshot> {

    @Query("SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.opponent_id LEFT JOIN assets a ON a.asset_id = s.asset_id WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): LiveData<List<SnapshotItem>>

    @Query("SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.opponent_id LEFT JOIN " +
        "assets a ON a.asset_id = s.asset_id WHERE s.asset_id = :assetId AND (s.type = :type OR s.type =:otherType) ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByType(assetId: String, type: String, otherType: String? = null): LiveData<List<SnapshotItem>>

    @Query("SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol  FROM snapshots s LEFT JOIN users u ON u.user_id = s.opponent_id LEFT JOIN assets a ON a.asset_id = s.asset_id WHERE s.asset_id = :assetId and snapshot_id = :snapshotId")
    fun snapshotLocal(assetId: String, snapshotId: String): SnapshotItem?

    @Query("SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.opponent_id LEFT JOIN assets a ON a.asset_id = s.asset_id ORDER BY created_at DESC")
    fun allSnapshots(): DataSource.Factory<Int, SnapshotItem>

    @Query("SELECT s.*, u.full_name AS opponentFullName, a.symbol AS asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.opponent_id LEFT JOIN " +
        "assets a ON a.asset_id = s.asset_id WHERE s.opponent_id = :opponentId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByUserId(opponentId: String): LiveData<List<SnapshotItem>>

    @Query("DELETE FROM snapshots WHERE type = 'pending'")
    fun clearPendingDeposits()

    @Query("DELETE FROM snapshots WHERE transaction_hash = :transactionHash")
    fun deleteSnapshotByHash(transactionHash: String)
}
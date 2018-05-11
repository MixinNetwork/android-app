package one.mixin.android.db

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomWarnings
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Dao
interface SnapshotDao : BaseDao<Snapshot> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT s.*, u.full_name as counterFullName, a.symbol as asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.counter_user_id LEFT JOIN assets a ON a.asset_id = s.asset_id WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): DataSource.Factory<Int, SnapshotItem>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT s.*, u.full_name as counterFullName, a.symbol as asset_symbol  FROM snapshots s LEFT JOIN users u ON u.user_id = s.counter_user_id LEFT JOIN assets a ON a.asset_id = s.asset_id WHERE s.asset_id = :assetId and snapshot_id = :snapshotId")
    fun snapshotLocal(assetId: String, snapshotId: String): SnapshotItem?

    @Query("SELECT s.*, u.full_name as counterFullName, a.symbol as asset_symbol FROM snapshots s LEFT JOIN users u ON u.user_id = s.counter_user_id LEFT JOIN assets a ON a.asset_id = s.asset_id ORDER BY created_at DESC")
    fun allSnapshots(): DataSource.Factory<Int, SnapshotItem>
}
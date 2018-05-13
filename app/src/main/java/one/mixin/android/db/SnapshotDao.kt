package one.mixin.android.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RoomWarnings
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem

@Dao
interface SnapshotDao : BaseDao<Snapshot> {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT s.*, u.full_name as counterFullName FROM snapshots s LEFT JOIN users u ON u.user_id = s.counter_user_id " +
        "WHERE asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): LiveData<List<SnapshotItem>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT s.*, u.full_name as counterFullName FROM snapshots s LEFT JOIN users u ON u.user_id = s.counter_user_id " +
        "WHERE asset_id = :assetId and snapshot_id = :snapshotId")
    fun snapshotLocal(assetId: String, snapshotId: String): SnapshotItem?
}
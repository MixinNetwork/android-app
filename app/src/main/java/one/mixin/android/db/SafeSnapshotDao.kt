package one.mixin.android.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import one.mixin.android.db.BaseDao.Companion.ESCAPE_SUFFIX
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.Token

@Dao
interface SafeSnapshotDao : BaseDao<SafeSnapshot> {
    companion object {
        const val SNAPSHOT_ITEM_PREFIX =
            """
                SELECT s.snapshot_id, s.type, s.asset_id, s.amount, s.created_at, s.opponent_id, s.trace_id, s.memo,
                s.confirmations, s.transaction_hash, s.opening_balance, s.closing_balance, s.deposit AS deposit, s.withdrawal AS withdrawal,
                u.avatar_url, u.full_name AS opponent_ful_name, t.symbol AS asset_symbol, t.confirmations AS asset_confirmations,
                i.inscription_hash, i.collection_hash, i.inscription_hash, ic.name, i.sequence, i.content_type, i.content_url, ic.icon_url 
                FROM safe_snapshots s 
                LEFT JOIN users u ON u.user_id = s.opponent_id 
                LEFT JOIN tokens t ON t.asset_id = s.asset_id
                LEFT JOIN inscription_items i on i.inscription_hash = s.inscription_hash
                LEFT JOIN inscription_collections ic on i.collection_hash = ic.collection_hash
            """
    }

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshots(assetId: String): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC LIMIT 20")
    fun snapshotsLimit(assetId: String): LiveData<List<SnapshotItem>>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY abs(s.amount) DESC, s.snapshot_id DESC")
    fun snapshotsOrderByAmount(assetId: String): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByType(
        assetId: String,
        type: String,
        otherType: String? = null,
    ): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY abs(s.amount) DESC, s.snapshot_id DESC")
    fun snapshotsByTypeOrderByAmount(
        assetId: String,
        type: String,
        otherType: String? = null,
    ): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsPaging(assetId: String): PagingSource<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByTypePaging(
        assetId: String,
        type: String,
        otherType: String? = null,
    ): PagingSource<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId ORDER BY abs(s.amount) DESC, s.snapshot_id DESC")
    fun snapshotsOrderByAmountPaging(assetId: String): PagingSource<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId AND s.type IN (:type, :otherType) ORDER BY abs(s.amount) DESC, s.snapshot_id DESC")
    fun snapshotsByTypeOrderByAmountPaging(
        assetId: String,
        type: String,
        otherType: String? = null,
    ): PagingSource<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.asset_id = :assetId and snapshot_id = :snapshotId")
    suspend fun snapshotLocal(
        assetId: String,
        snapshotId: String,
    ): SnapshotItem?

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE snapshot_id = :snapshotId")
    suspend fun findSnapshotById(snapshotId: String): SnapshotItem?

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE trace_id = :traceId")
    suspend fun findSnapshotByTraceId(traceId: String): SnapshotItem?

    @RawQuery(observedEntities = [SafeSnapshot::class, User::class, Token::class, InscriptionItem::class, InscriptionCollection::class])
    fun getSnapshots(query: SupportSQLiteQuery): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX ORDER BY abs(s.amount * t.price_usd) DESC")
    fun allSnapshotsOrderByAmount(): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.type IN (:type, :otherType) ORDER BY s.created_at DESC")
    fun allSnapshotsByType(
        type: String,
        otherType: String? = null,
    ): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.type IN (:type, :otherType) ORDER BY abs(s.amount * t.price_usd) DESC")
    fun allSnapshotsByTypeOrderByAmount(
        type: String,
        otherType: String? = null,
    ): DataSource.Factory<Int, SnapshotItem>

    @Query("$SNAPSHOT_ITEM_PREFIX WHERE s.opponent_id = :opponentId AND s.type != 'pending' ORDER BY s.created_at DESC, s.snapshot_id DESC")
    fun snapshotsByUserId(opponentId: String): DataSource.Factory<Int, SnapshotItem>

    @Query("DELETE FROM safe_snapshots WHERE type = 'pending'")
    suspend fun clearAllPendingDeposits()

    @Query("DELETE FROM safe_snapshots WHERE asset_id = :assetId AND type = 'pending'")
    suspend fun clearPendingDepositsByAssetId(assetId: String)

    @Query("DELETE FROM safe_snapshots WHERE type = 'pending' AND deposit LIKE '%' || :depositHash || '%' $ESCAPE_SUFFIX")
    fun deletePendingSnapshotByHash(depositHash: String)

    @Query("SELECT sn.* FROM safe_snapshots sn WHERE sn.rowid > :rowId ORDER BY sn.rowid ASC LIMIT :limit")
    fun getSnapshotByLimitAndRowId(
        limit: Int,
        rowId: Long,
    ): List<SafeSnapshot>

    @Query("SELECT inscription_hash FROM safe_snapshots WHERE snapshot_id = :snapshotId")
    suspend fun findHashBySnapshotId(snapshotId: String): String?

    @Query("SELECT rowid FROM safe_snapshots WHERE snapshot_id = :snapshotId")
    fun getSnapshotRowId(snapshotId: String): Long?

    @Query("SELECT count(1) FROM safe_snapshots")
    fun countSnapshots(): Long

    @Query("SELECT count(1) FROM safe_snapshots WHERE rowid > :rowId")
    fun countSnapshots(rowId: Long): Long

    @Query("SELECT created_at FROM safe_snapshots WHERE asset_id =:assetId ORDER BY created_at DESC LIMIT 1")
    fun getLastItemCreate(assetId: String): String?
}

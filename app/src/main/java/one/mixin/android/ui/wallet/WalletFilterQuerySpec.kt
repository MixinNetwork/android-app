package one.mixin.android.ui.wallet

import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.codegen.annotation.GeneratedSimpleSQLiteQuery
import one.mixin.android.db.SafeSnapshotDao.Companion.SNAPSHOT_ITEM_PREFIX

private const val WEB3_TRANSACTION_QUERY_SQL =
    """
    SELECT DISTINCT w.transaction_hash, w.transaction_type, w.status, w.block_number, w.chain_id,
    w.address, w.fee, w.senders, w.receivers, w.approvals, w.send_asset_id, w.receive_asset_id,
    w.transaction_at, w.updated_at, w.level,
    c.symbol as chain_symbol,
    c.icon_url as chain_icon_url,
    s.icon_url as send_asset_icon_url,
    s.symbol as send_asset_symbol,
    r.icon_url as receive_asset_icon_url,
    r.symbol as receive_asset_symbol
    FROM transactions w
    LEFT JOIN tokens c ON c.asset_id = w.chain_id AND c.wallet_id = '{{walletId}}'
    LEFT JOIN tokens s ON s.asset_id = w.send_asset_id AND s.wallet_id = '{{walletId}}'
    LEFT JOIN tokens r ON r.asset_id = w.receive_asset_id AND r.wallet_id = '{{walletId}}'
    {{whereSql}} {{orderSql}}
    """

@GeneratedQueryProvider(generatedName = "WalletFilterQueryGenerated")
interface WalletFilterQuerySpec {
    @GeneratedSimpleSQLiteQuery(
        sql = SNAPSHOT_ITEM_PREFIX + "{{whereSql}} {{orderSql}}",
    )
    fun snapshots(
        whereSql: String,
        orderSql: String,
    ): SimpleSQLiteQuery

    @GeneratedSimpleSQLiteQuery(
        sql = WEB3_TRANSACTION_QUERY_SQL,
    )
    fun web3Transactions(
        whereSql: String,
        orderSql: String,
        walletId: String,
    ): SimpleSQLiteQuery

    @GeneratedSimpleSQLiteQuery(
        sql = "SELECT * FROM orders o {{whereSql}} {{orderSql}}",
    )
    fun orders(
        whereSql: String,
        orderSql: String,
    ): SimpleSQLiteQuery
}

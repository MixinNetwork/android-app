package one.mixin.android.ui.wallet

import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.tip.wc.SortOrder
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class Web3FilterParams(
    var order: SortOrder = SortOrder.Recent,
    var type: SnapshotType = SnapshotType.all,
    var tokenItems: List<Web3TokenItem>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null,
) {
    override fun toString(): String {
        return "order:${order.name} type:${type.name} tokens:${tokenItems?.map { it.symbol }} " +
            "startTime:${startTime?.let { Instant.ofEpochMilli(it) } ?: ""} " +
            "endTime:${endTime?.let { Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000) } ?: ""}"
    }

    val selectTime: String?
        get() {
            if (startTime == null || endTime == null) return null
            else {
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                    .withZone(ZoneId.systemDefault())
                val start = Instant.ofEpochMilli(startTime!!).atZone(ZoneId.systemDefault()).toLocalDate()
                val end = Instant.ofEpochMilli(endTime!!).atZone(ZoneId.systemDefault()).toLocalDate()
                return "${formatter.format(start)} - ${formatter.format(end)}"
            }
        }

    val typeTitle: Int
        get() {
            return when (type) {
                SnapshotType.all -> R.string.All
                SnapshotType.withdrawal -> R.string.Withdrawal
                SnapshotType.deposit -> R.string.Deposit
                SnapshotType.snapshot -> R.string.Transfer
            }
        }
        
    fun formatDate(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(timestamp))
    }

    fun buildQuery(): SimpleSQLiteQuery {
        val filters = mutableListOf<String>()

        if (type != SnapshotType.all) {
            when (type) {
                SnapshotType.snapshot -> {
                    filters.add("(deposit IS NULL OR deposit = 'null')")
                    filters.add("(withdrawal IS NULL OR withdrawal = 'null')")
                }
                SnapshotType.deposit -> {
                    filters.add("deposit IS NOT NULL")
                    filters.add("deposit != 'null'")
                }
                SnapshotType.withdrawal -> {
                    filters.add("withdrawal IS NOT NULL")
                    filters.add("withdrawal != 'null'")
                }
                else -> {}
            }
        }

        tokenItems?.let {
            if (it.isNotEmpty()) {
                val tokenIds = it.joinToString(", ") { token -> "'${token.assetId}'" }
                filters.add("w.asset_id IN ($tokenIds)")
            }
        }

        startTime?.let {
            filters.add("w.created_at >= '${Instant.ofEpochMilli(it)}'")
        }

        endTime?.let {
            filters.add("w.created_at <= '${Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
        }
        
        val whereSql = if (filters.isEmpty()) {
            ""
        } else {
            "WHERE ${filters.joinToString(" AND ")}"
        }

        val orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY w.created_at DESC"
            SortOrder.Oldest -> "ORDER BY w.created_at ASC"
            // SortOrder.Value -> "ORDER BY abs(amount * price_usd) DESC" // todo
            SortOrder.Amount -> "ORDER BY w.amount DESC"
            else -> ""
        }

        return SimpleSQLiteQuery(
            "SELECT w.transaction_id, w.transaction_hash, w.output_index, w.block_number, " +
                "w.sender, w.receiver, w.output_hash, w.chain_id, w.asset_id, w.amount, " +
                "w.created_at, w.updated_at, t.symbol, t.icon_url " +
                "FROM web3_transactions w " +
                "LEFT JOIN web3_tokens t on t.asset_id = w.asset_id $whereSql $orderSql"
        )
    }
}

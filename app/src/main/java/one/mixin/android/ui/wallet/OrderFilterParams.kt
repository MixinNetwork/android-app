package one.mixin.android.ui.wallet

import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.db.web3.vo.Web3TokenItem
import org.threeten.bp.Instant

class OrderFilterParams(
    var order: SortOrder = SortOrder.Recent,
    var tokenItems: List<Web3TokenItem>? = null,
    var walletIds: List<String>? = null,
    var statuses: List<String>? = null,
    var fundStatuses: List<String>? = null,
    var orderTypes: List<String>? = null, // "swap" or "limit"
    var startTime: Long? = null,
    var endTime: Long? = null,
) {
    override fun toString(): String {
        return "order:${order.name} tokens:${tokenItems?.map { it.symbol }} statuses:${statuses} fundStatuses:${fundStatuses} startTime:${startTime?.let { Instant.ofEpochMilli(it) } ?: ""} endTime:${endTime?.let { Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000) } ?: ""}"
    }

    fun buildQuery(): SimpleSQLiteQuery {
        val filters = mutableListOf<String>()
        tokenItems?.let {
            if (it.isNotEmpty()) {
                val tokenIds = it.joinToString(", ") { t -> "'${t.assetId}'" }
                filters.add("(o.pay_asset_id IN ($tokenIds) OR o.receive_asset_id IN ($tokenIds))")
            }
        }
        walletIds?.let { list ->
            if (list.isNotEmpty()) {
                val s = list.joinToString(", ") { v -> "'$v'" }
                filters.add("o.wallet_id IN ($s)")
            }
        }
        statuses?.let { list ->
            if (list.isNotEmpty()) {
                val s = list.joinToString(", ") { v -> "'$v'" }
                filters.add("o.state IN ($s)")
            }
        }
        orderTypes?.let { list ->
            if (list.isNotEmpty()) {
                val s = list.joinToString(", ") { v -> "'$v'" }
                filters.add("o.order_type IN ($s)")
            }
        }
        fundStatuses?.let { list ->
            if (list.isNotEmpty()) {
                val s = list.joinToString(", ") { v -> "'$v'" }
                filters.add("o.fund_status IN ($s)")
            }
        }
        startTime?.let {
            filters.add("o.created_at >= '${Instant.ofEpochMilli(it)}'")
        }
        endTime?.let {
            filters.add("o.created_at <= '${Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
        }
        val whereSql = if (filters.isEmpty()) "" else "WHERE ${filters.joinToString(" AND ")}"
        val orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY o.created_at DESC"
            SortOrder.Oldest -> "ORDER BY o.created_at ASC"
            else -> "ORDER BY o.created_at DESC"
        }
        val sql = "SELECT * FROM orders o $whereSql $orderSql"
        return SimpleSQLiteQuery(sql)
    }
}

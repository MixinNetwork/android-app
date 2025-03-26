package one.mixin.android.ui.wallet

import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.tip.wc.SortOrder
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class Web3FilterParams(
    var order: SortOrder = SortOrder.Recent,
    var tokenFilterType: Web3TokenFilterType = Web3TokenFilterType.ALL,
    var tokenItems: List<Web3TokenItem>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null,
) {
    override fun toString(): String {
        return "order:${order.name} tokenFilterType:${tokenFilterType.name} tokens:${tokenItems?.map { it.symbol }} " +
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

    fun formatDate(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(timestamp))
    }

    fun buildQuery(): SimpleSQLiteQuery {
        val filters = mutableListOf<String>()

        tokenItems?.let {
            if (it.isNotEmpty()) {
                val tokenIds = it.joinToString(", ") { token -> "'${token.assetId}'" }
                filters.add("w.asset_id IN ($tokenIds)")
            }
        }

        tokenFilterType.let {
            when (it) {
                Web3TokenFilterType.SEND -> filters.add("w.transaction_type = 'send'")
                Web3TokenFilterType.RECEIVE -> filters.add("w.transaction_type = 'receive'")
                Web3TokenFilterType.CONTRACT -> filters.add("w.transaction_type = 'contract'")
                else ->  {}
            }
        }

        startTime?.let {
            filters.add("w.transaction_at >= '${Instant.ofEpochMilli(it)}'")
        }

        endTime?.let {
            filters.add("w.transaction_at <= '${Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
        }
        
        val whereSql = if (filters.isEmpty()) {
            ""
        } else {
            "WHERE ${filters.joinToString(" AND ")}"
        }

        val orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY w.transaction_at DESC"
            SortOrder.Oldest -> "ORDER BY w.transaction_at ASC"
            SortOrder.Value -> "ORDER BY abs(w.amount * t.price_usd) DESC"
            SortOrder.Amount -> "ORDER BY w.amount DESC"
            else -> ""
        }

        return SimpleSQLiteQuery(
            "SELECT w.transaction_id, w.transaction_type, w.transaction_hash, w.output_index, w.block_number, " +
                "w.sender, w.receiver, w.output_hash, w.chain_id, w.asset_id, w.amount, " +
                "w.transaction_at, w.updated_at, w.transaction_type, w.status, t.symbol, t.icon_url, w.status " +
                "FROM transactions w " +
                "LEFT JOIN tokens t on t.asset_id = w.asset_id $whereSql $orderSql"
        )
    }
}

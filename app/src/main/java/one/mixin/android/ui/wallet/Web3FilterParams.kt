package one.mixin.android.ui.wallet

import android.os.Parcelable
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.tip.wc.SortOrder
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@Parcelize
class Web3FilterParams(
    var order: SortOrder = SortOrder.Recent,
    var tokenFilterType: Web3TokenFilterType = Web3TokenFilterType.ALL,
    var tokenItems: List<Web3TokenItem>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null,
    var level: Int = 0b00
) : Parcelable {
    companion object {
        const val FILTER_MASK = 0b11
        const val FILTER_GOOD_ONLY = 0b00
        const val FILTER_GOOD_AND_UNKNOWN = 0b10
        const val FILTER_GOOD_AND_SPAM = 0b01
        const val FILTER_ALL = 0b11
    }

    override fun toString(): String {
        return "order:${order.name} tokenFilterType:${tokenFilterType.name} tokens:${tokenItems?.map { it.symbol }} " +
            "startTime:${startTime?.let { Instant.ofEpochMilli(it) } ?: ""} " +
            "endTime:${endTime?.let { Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000) } ?: ""} " +
            "level:$level"
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
                filters.add("(w.send_asset_id IN ($tokenIds) OR w.receive_asset_id IN ($tokenIds))")
            }
        }

        tokenFilterType.let {
            when (it) {
                Web3TokenFilterType.SEND -> filters.add("w.transaction_type = 'transfer_out'")
                Web3TokenFilterType.RECEIVE -> filters.add("w.transaction_type = 'transfer_in'")
                Web3TokenFilterType.APPROVAL -> filters.add("w.transaction_type = 'approval'")
                Web3TokenFilterType.SWAP -> filters.add("w.transaction_type = 'swap'")
                Web3TokenFilterType.PENDING -> filters.add("w.status = '${TransactionStatus.PENDING.value}'")
                Web3TokenFilterType.ALL -> {}
            }
        }

        startTime?.let {
            filters.add("w.transaction_at >= '${Instant.ofEpochMilli(it)}'")
        }

        endTime?.let {
            filters.add("w.transaction_at <= '${Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
        }

        when (level and FILTER_MASK) {
            FILTER_GOOD_ONLY -> filters.add("w.level >= 11") // Good
            FILTER_GOOD_AND_UNKNOWN -> filters.add("w.level >= 10") // Good + Unknown
            FILTER_GOOD_AND_SPAM -> filters.add("(w.level >= 11 OR w.level <= 1)") // Good + Spam
            FILTER_ALL -> { /* Good + Unknown + Spam */ }
        }

        val whereSql = if (filters.isEmpty()) {
            ""
        } else {
            "WHERE ${filters.joinToString(" AND ")}"
        }

        val orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY w.transaction_at DESC"
            SortOrder.Oldest -> "ORDER BY w.transaction_at ASC"
            else -> ""
        }

        return SimpleSQLiteQuery(
            "SELECT w.transaction_hash, w.transaction_type, w.status, w.block_number, w.chain_id, " +
                "w.address, w.fee, w.senders, w.receivers, w.approvals, w.send_asset_id, w.receive_asset_id, " +
                "w.transaction_at, w.updated_at, " +
                "c.symbol as chain_symbol, " +
                "c.icon_url as chain_icon_url, " +
                "s.icon_url as send_asset_icon_url, " +
                "s.symbol as send_asset_symbol, " +
                "r.icon_url as receive_asset_icon_url, " +
                "r.symbol as receive_asset_symbol " +
                "FROM transactions w " +
                "LEFT JOIN tokens c ON c.asset_id = w.chain_id " +
                "LEFT JOIN tokens s ON s.asset_id = w.send_asset_id " +
                "LEFT JOIN tokens r ON r.asset_id = w.receive_asset_id " +
                "$whereSql $orderSql"
        )
    }
}


package one.mixin.android.ui.wallet

import androidx.sqlite.db.SimpleSQLiteQuery
import one.mixin.android.R
import one.mixin.android.db.SafeSnapshotDao.Companion.SNAPSHOT_ITEM_PREFIX
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.displayAddress
import one.mixin.android.vo.safe.TokenItem
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class Web3FilterParams(
    var order: SortOrder = SortOrder.Recent,
    var type: SnapshotType = SnapshotType.all,
    var tokenItems: List<TokenItem>? = null,
    var recipients: List<Recipient>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null,
) {
    override fun toString(): String {
        return "order:${order.name} type:${type.name} tokens:${tokenItems?.map { it.symbol }} users${recipients?.map { it }} startTime:${startTime?.let { Instant.ofEpochMilli(it) } ?: ""} endTime:${endTime?.let { Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000) } ?: ""}"
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

    fun buildQuery(): SimpleSQLiteQuery {
        val filters = mutableListOf<String>()

        if (type != SnapshotType.all) {
            when(type){
                SnapshotType.snapshot -> {
                    filters.add("(s.deposit IS NULL OR s.deposit = 'null')")
                    filters.add("(s.withdrawal IS NULL OR s.withdrawal = 'null')")
                }

                SnapshotType.deposit -> {
                    filters.add("s.deposit IS NOT NULL")
                    filters.add("s.deposit != 'null'")
                }

                SnapshotType.withdrawal -> {
                    filters.add("s.withdrawal IS NOT NULL")
                    filters.add("s.withdrawal != 'null'")
                }
                else->{}
            }
        }

        tokenItems?.let {
            if (it.isNotEmpty()) {
                val tokenIds = it.joinToString(", ") { token -> "'${token.assetId}'" }
                filters.add("s.asset_id IN ($tokenIds)")
            }
        }

        recipients?.let { list ->
            val users = list.mapNotNull { item ->
                item as? UserItem
            }
            val userSql = if (users.isNotEmpty()) {
                users.joinToString(", ") { user -> "'${user.id}'" }.let {userIds->
                    "s.opponent_id IN ($userIds)"
                }
            } else {
                null
            }

            val addresses = list.mapNotNull { item ->
                item as? AddressItem
            }
            val addressSql = if (addresses.isNotEmpty()) {
                 addresses.joinToString(" OR ") { item ->
                    "s.withdrawal LIKE '%${item.displayAddress()}%' OR s.deposit LIKE '%${item.displayAddress()}%'"
                }
            } else {
                null
            }
            if (userSql != null && addressSql != null) {
                filters.add("($userSql OR $addressSql)")
            } else if (userSql != null) {
                filters.add("($userSql)")
            } else if (addressSql != null) {
                filters.add("($addressSql)")
            } else {
                // do nothing
            }
        }

        startTime?.let {
            filters.add("s.created_at >= '${Instant.ofEpochMilli(it)}'")
        }

        endTime?.let {
            filters.add("s.created_at <= '${Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
        }
        val whereSql = if (filters.isEmpty()) {
            ""
        } else {
            "WHERE ${filters.joinToString(" AND ")}"
        }

        val orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY s.created_at DESC"
            SortOrder.Oldest -> "ORDER BY s.created_at ASC"
            SortOrder.Value -> "ORDER BY abs(s.amount * t.price_usd) DESC"
            SortOrder.Amount -> "ORDER BY s.amount DESC"
            else -> ""
        }

        return SimpleSQLiteQuery("$SNAPSHOT_ITEM_PREFIX $whereSql $orderSql")
    }
}

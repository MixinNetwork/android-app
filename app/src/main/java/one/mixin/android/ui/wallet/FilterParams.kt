package one.mixin.android.ui.wallet

import androidx.room3.RoomRawQuery
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Address
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.displayAddress
import one.mixin.android.vo.safe.TokenItem
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class FilterParams(
    var order: SortOrder = SortOrder.Recent,
    var type: SnapshotType = SnapshotType.all,
    var tokenItems: List<TokenItem>? = null,
    var recipients: List<Recipient>? = null,
    var startTime: Long? = null,
    var endTime: Long? = null,
    var address: Address? = null,
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
                SnapshotType.pending -> R.string.Pending
            }
        }

    fun buildQuery(limit: Int? = null): RoomRawQuery {
        val filters = mutableListOf<String>()

        address?.let {
            filters.add("t.chain_id = '${it.chainId.replace("'", "''")}'")
            filters.add("(${it.jsonAddressPredicate("s.deposit", "sender")} OR ${it.jsonAddressPredicate("s.withdrawal", "receiver")})")
        }

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

                SnapshotType.pending -> {
                    filters.add("s.type == 'pending'")
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

        var orderSql = when (order) {
            SortOrder.Recent -> "ORDER BY s.created_at DESC"
            SortOrder.Oldest -> "ORDER BY s.created_at ASC"
            SortOrder.Value -> "ORDER BY abs(s.amount * t.price_usd) DESC"
            SortOrder.Amount -> "ORDER BY s.amount DESC"
            else -> ""
        }
        if (address != null) {
            orderSql += when (order) {
                SortOrder.Recent -> ", s.snapshot_id DESC"
                SortOrder.Oldest -> ", s.snapshot_id ASC"
                SortOrder.Value, SortOrder.Amount -> ", s.created_at DESC, s.snapshot_id DESC"
                else -> "ORDER BY s.created_at DESC, s.snapshot_id DESC"
            }
        }
        limit?.let {
            require(it >= 0)
            orderSql += " LIMIT $it"
        }

        return WalletFilterQueryGenerated.snapshots(whereSql, orderSql)
    }
}

internal fun Address.jsonAddressPredicate(column: String, field: String): String {
    val ignoreCase = chainId in Constants.Web3EvmChainIds
    val marker = "\"$field\":"
    if (tag.isNullOrEmpty()) {
        val needle = (marker + GsonHelper.customGson.toJson(destination)).replace("'", "''")
        return if (ignoreCase) {
            "instr(lower($column), lower('$needle')) > 0"
        } else {
            "instr($column, '$needle') > 0"
        }
    }

    // Privacy converters have one endpoint; only its destination may ignore ASCII case.
    val destinationJson = GsonHelper.customGson.toJson(destination).dropLast(1).replace("'", "''")
    val tagJson = GsonHelper.customGson.toJson(":$tag").drop(1).replace("'", "''")
    val start = "instr($column, '$marker') + ${marker.length}"
    val collation = if (ignoreCase) "NOCASE" else "BINARY"
    return "(instr($column, '$marker') > 0 AND " +
        "substr($column, $start, length('$destinationJson')) = '$destinationJson' COLLATE $collation AND " +
        "substr($column, $start + length('$destinationJson'), length('$tagJson')) = '$tagJson' COLLATE BINARY)"
}

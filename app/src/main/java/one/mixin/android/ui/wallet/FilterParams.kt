package one.mixin.android.ui.wallet

import one.mixin.android.R
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import org.threeten.bp.Instant

class FilterParams {
    var order: SortOrder = SortOrder.Recent
    var type: SnapshotType = SnapshotType.All
    var tokenItems: List<TokenItem>? = null
    var users: List<User>? = null
    var startTime: Long? = null
    var endTime: Long? = null

    override fun toString(): String {
        return "order:${order.name} type:${type.name} tokens:${tokenItems?.map { it.symbol }} users${users?.map { it.fullName }} startTime:${startTime?.let { Instant.ofEpochMilli(it) } ?: ""} endTime:${endTime?.let { Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000) } ?: ""}"
    }

    val typeTitle: Int
        get() {
            return when (type) {
                SnapshotType.All -> R.string.All
                SnapshotType.Withdrawal -> R.string.Withdrawal
                SnapshotType.Deposit -> R.string.Deposit
                SnapshotType.Transfer -> R.string.Transfer
            }
        }
}

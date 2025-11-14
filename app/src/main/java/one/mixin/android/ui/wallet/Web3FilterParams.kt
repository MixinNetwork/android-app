package one.mixin.android.ui.wallet

import android.os.Parcelable
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.Web3TokenItem
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
    var level: Int = 0b00,
    var walletId: String
) : Parcelable {
    companion object {
        const val FILTER_MASK = 0b11
        const val FILTER_GOOD_ONLY = 0b00
        const val FILTER_GOOD_AND_UNKNOWN = 0b10
        const val FILTER_GOOD_AND_SPAM = 0b01
        const val FILTER_ALL = 0b11
    }

    override fun toString(): String {
        return "order:${order.name} tokenFilterType:${tokenFilterType.name} tokens:${tokenItems?.map { it.symbol }} walletId:{$walletId}" +
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
}


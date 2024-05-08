package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import one.mixin.android.extension.hexString
import one.mixin.android.extension.isByteArrayValidUtf8
import one.mixin.android.extension.isValidHex
import one.mixin.android.vo.safe.SafeDeposit
import one.mixin.android.vo.safe.SafeSnapshot
import one.mixin.android.vo.safe.SafeSnapshotType
import one.mixin.android.vo.safe.SafeWithdrawal

@SuppressLint("ParcelCreator")
@Parcelize
@Entity
data class SnapshotItem(
    @PrimaryKey
    @SerializedName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String,
    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String,
    @SerializedName("opponent_ful_name")
    @ColumnInfo(name = "opponent_ful_name")
    val opponentFullName: String?,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String?,
    @SerializedName("memo")
    @ColumnInfo(name = "memo")
    val memo: String?,
    @SerializedName("asset_symbol")
    @ColumnInfo(name = "asset_symbol")
    val assetSymbol: String?,
    @SerializedName("confirmations")
    @ColumnInfo(name = "confirmations")
    val confirmations: Int?,
    @SerializedName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @SerializedName("asset_confirmations")
    @ColumnInfo(name = "asset_confirmations")
    val assetConfirmations: Int,
    @SerializedName("trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String?,
    @SerializedName("opening_balance")
    @ColumnInfo(name = "opening_balance")
    val openingBalance: String?,
    @SerializedName("closing_balance")
    @ColumnInfo(name = "closing_balance")
    val closingBalance: String?,
    @SerializedName("deposit")
    @SerialName("deposit")
    @ColumnInfo(name = "deposit")
    val deposit: SafeDeposit?,
    @SerializedName("withdrawal")
    @SerialName("withdrawal")
    @ColumnInfo(name = "withdrawal")
    val withdrawal: SafeWithdrawal?,
    @SerializedName("label")
    @ColumnInfo(name = "label")
    var label: String?,
    @SerializedName("inscription_hash")
    @ColumnInfo(name = "inscription_hash")
    val inscriptionHash: String?,
    @SerializedName("label")
    @ColumnInfo(name = "collection_hash")
    val collectionHash: String?,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String?,
    @SerializedName("sequence")
    @ColumnInfo(name = "sequence")
    val sequence: Long?,
    @SerializedName("content_type")
    @ColumnInfo(name = "content_type")
    val contentType: String?,
    @SerializedName("content_url")
    @ColumnInfo(name = "content_url")
    val contentUrl: String?,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
) : Parcelable {
    val formatMemo: FormatMemo?
        get() {
            return if (memo.isNullOrBlank()) {
                null
            } else {
                FormatMemo(memo)
            }
        }

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<SnapshotItem>() {
                override fun areItemsTheSame(
                    oldItem: SnapshotItem,
                    newItem: SnapshotItem,
                ) =
                    oldItem.snapshotId == newItem.snapshotId

                override fun areContentsTheSame(
                    oldItem: SnapshotItem,
                    newItem: SnapshotItem,
                ) =
                    oldItem == newItem
            }
    }

    fun simulateType(): SafeSnapshotType =
        if (type == SafeSnapshotType.pending.name) {
            SafeSnapshotType.pending
        } else if (deposit != null) {
            SafeSnapshotType.deposit
        } else if (withdrawal != null) {
            SafeSnapshotType.withdrawal
        } else {
            SafeSnapshotType.snapshot
        }

    fun hasTransactionDetails(): Boolean {
        if (type == SafeSnapshotType.withdrawal.name) {
            return withdrawal?.receiver.isNullOrBlank() || withdrawal?.withdrawalHash.isNullOrBlank()
        } else if (type == SafeSnapshotType.deposit.name) {
            return deposit?.sender.isNullOrBlank()
        }
        return true
    }
}

@Parcelize
class FormatMemo(var utf: String?, var hex: String?) : Parcelable {
    constructor(input: String) : this(null, null) {
        if (input.isValidHex()) {
            val byteArray = input.chunked(2) { it.toString().toInt(16).toByte() }.toByteArray()
            if (byteArray.isByteArrayValidUtf8()) {
                utf = String(byteArray)
                hex = input
            } else {
                hex = input
            }
        } else {
            utf = input
            hex = input.toByteArray().hexString()
        }
    }
}

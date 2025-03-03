package one.mixin.android.db.web3.vo

import android.content.Context
import android.os.Parcelable
import android.text.SpannedString
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize
import one.mixin.android.R
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.numberFormat2
import java.math.BigDecimal

@Parcelize
data class Web3TransactionItem(
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,
    
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,
    
    @ColumnInfo(name = "output_index")
    val outputIndex: Int,
    
    @ColumnInfo(name = "block_number")
    val blockNumber: Long,
    
    @ColumnInfo(name = "sender")
    val sender: String,
    
    @ColumnInfo(name = "receiver")
    val receiver: String,
    
    @ColumnInfo(name = "output_hash")
    val outputHash: String,
    
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    
    @ColumnInfo(name = "amount")
    val amount: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "symbol")
    val symbol: String = "",

    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
) : Parcelable {

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Web3TransactionItem>() {
                override fun areItemsTheSame(
                    oldItem: Web3TransactionItem,
                    newItem: Web3TransactionItem,
                ) =
                    oldItem.transactionId == newItem.transactionId

                override fun areContentsTheSame(
                    oldItem: Web3TransactionItem,
                    newItem: Web3TransactionItem,
                ) =
                    oldItem == newItem
            }
    }

    fun getFormattedAmount(): String {
        return try {
            BigDecimal(amount).numberFormat2()
        } catch (e: Exception) {
            amount
        }
    }

    fun isSend(address: String): Boolean {
        return sender.equals(address, ignoreCase = true)
    }

    fun isReceive(address: String): Boolean {
        return receiver.equals(address, ignoreCase = true) && !sender.equals(
            address,
            ignoreCase = true
        )
    }

    fun getDirectionText(context: Context, address: String): SpannedString {
        return when {
            isSend(address) -> buildAmountSymbol(
                context,
                "-${getFormattedAmount()}",
                symbol,
                context.resources.getColor(R.color.wallet_pink, null),
                context.colorFromAttribute(R.attr.text_primary)
            )

            isReceive(address) -> buildAmountSymbol(
                context,
                "+${getFormattedAmount()}",
                symbol,
                context.resources.getColor(R.color.wallet_green, null),
                context.colorFromAttribute(R.attr.text_primary)
            )

            else -> SpannedString(getFormattedAmount())
        }
    }
}

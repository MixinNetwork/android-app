package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.numberFormat8
import java.math.BigDecimal

@Parcelize
data class Web3TransactionItem(
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String,

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
    
    @ColumnInfo(name = "transaction_at")
    val transactionAt: String,
    
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
            BigDecimal(amount).numberFormat8()
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

}

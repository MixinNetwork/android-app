package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.AssetChangeListConverter
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.numberFormat8
import java.math.BigDecimal

@Parcelize
data class Web3TransactionItem(
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String,

    @ColumnInfo(name = "status")
    val status: String,
    
    @ColumnInfo(name = "block_number")
    val blockNumber: Long,
    
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    
    @ColumnInfo(name = "fee")
    val fee: String,
    
    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "senders")
    val senders: List<AssetChange>,

    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "receivers")
    val receivers: List<AssetChange>,

    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "approvals")
    val approvals: List<AssetChange>? = null,
    
    @ColumnInfo(name = "send_asset_id")
    val sendAssetId: String? = null,

    @ColumnInfo(name = "receive_asset_id")
    val receiveAssetId: String? = null,
    
    @ColumnInfo(name = "transaction_at")
    val transactionAt: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "chain_symbol")
    val chainSymbol: String? = null,

    @ColumnInfo(name = "chain_icon_url")
    val chainIconUrl: String? = null,
    
    @ColumnInfo(name = "asset_id")
    val assetId: String = "",
    
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    
    @ColumnInfo(name = "symbol")
    val symbol: String? = null
) : Parcelable {

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Web3TransactionItem>() {
                override fun areItemsTheSame(
                    oldItem: Web3TransactionItem,
                    newItem: Web3TransactionItem,
                ) =
                    oldItem.transactionHash == newItem.transactionHash

                override fun areContentsTheSame(
                    oldItem: Web3TransactionItem,
                    newItem: Web3TransactionItem,
                ) =
                    oldItem == newItem
            }
    }

    fun getMainAmount(): String {
        return when (transactionType) {
            TransactionType.TRANSFER_IN.value -> {
                if (receivers.isNotEmpty()) receivers[0].amount else "0"
            }
            TransactionType.TRANSFER_OUT.value -> {
                if (senders.isNotEmpty()) senders[0].amount else "0"
            }
            TransactionType.SWAP.value -> {
                if (receivers.isNotEmpty()) receivers[0].amount else "0"
            }
            TransactionType.APPROVAL.value -> {
                if (approvals != null && approvals.isNotEmpty()) approvals[0].amount else "0"
            }
            else -> "0"
        }
    }

    fun getFormattedAmount(): String {
        return try {
            BigDecimal(getMainAmount()).numberFormat12()
        } catch (e: Exception) {
            getMainAmount()
        }
    }

    fun getMainAssetId(): String {
        return when (transactionType) {
            TransactionType.TRANSFER_IN.value -> {
                receiveAssetId ?: if (receivers.isNotEmpty()) receivers[0].assetId else chainId
            }
            TransactionType.TRANSFER_OUT.value -> {
                sendAssetId ?: if (senders.isNotEmpty()) senders[0].assetId else chainId
            }
            TransactionType.SWAP.value -> {
                receiveAssetId ?: if (receivers.isNotEmpty()) receivers[0].assetId else chainId
            }
            TransactionType.APPROVAL.value -> {
                if (approvals != null && approvals.isNotEmpty()) approvals[0].assetId else chainId
            }
            else -> chainId
        }
    }

    fun isSend(address: String): Boolean {
        return senders.any { it.from?.equals(address, ignoreCase = true) == true }
    }

    fun isReceive(address: String): Boolean {
        return receivers.any { it.to?.equals(address, ignoreCase = true) == true } && 
               !senders.any { it.from?.equals(address, ignoreCase = true) == true }
    }
}

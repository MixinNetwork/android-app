package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.db.converter.ListConverter
import one.mixin.android.vo.WalletCategory

enum class SafeChain(val value: String, val chainId: String) {
    BITCOIN("1", Constants.ChainId.BITCOIN_CHAIN_ID),
    ETHEREUM("2", Constants.ChainId.ETHEREUM_CHAIN_ID),
    LITECOIN("5", Constants.ChainId.Litecoin),
    POLYGON("6", Constants.ChainId.Polygon);

    companion object {
        fun fromValue(value: String?): SafeChain? {
            if (value.isNullOrEmpty()) return null
            return entries.firstOrNull { it.value == value }
        }
    }
}

@TypeConverters(ListConverter::class)
@Entity(tableName = "wallets")
@Parcelize
data class Web3Wallet(
    @PrimaryKey
    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val id: String,

    @ColumnInfo(name = "category")
    @SerializedName("category")
    val category: String,

    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "owners")
    @SerializedName("owners")
    val owners: List<String>?,

    @ColumnInfo(name = "safe_chain_id")
    @SerializedName("safe_chain_id")
    val safeChainId: String?,

    @ColumnInfo("safe_address")
    @SerializedName("safe_address")
    val safeAddress: String?,

    @ColumnInfo("safe_url")
    @SerializedName("safe_url")
    val safeUrl: String?,
) : Parcelable {
    @Ignore
    var hasLocalPrivateKey: Boolean = false

    val safeChain: SafeChain?
        get() = SafeChain.fromValue(safeChainId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Web3Wallet) return false

        return id == other.id &&
                category == other.category &&
                name == other.name &&
                createdAt == other.createdAt &&
                updatedAt == other.updatedAt &&
                hasLocalPrivateKey == other.hasLocalPrivateKey
    }
}

fun Web3Wallet.isTransferFeeFree() : Boolean {
    return category == WalletCategory.CLASSIC.value || (isImported() && hasLocalPrivateKey)
}

fun Web3Wallet.notClassic(): Boolean {
    return category == WalletCategory.IMPORTED_MNEMONIC.value || category == WalletCategory.IMPORTED_PRIVATE_KEY.value || category == WalletCategory.WATCH_ADDRESS.value || category == WalletCategory.MIXIN_SAFE.value
}

fun Web3Wallet.isImported(): Boolean {
    return category == WalletCategory.IMPORTED_MNEMONIC.value || category == WalletCategory.IMPORTED_PRIVATE_KEY.value
}

fun Web3Wallet.isWatch(): Boolean {
    return category == WalletCategory.WATCH_ADDRESS.value
}

fun Web3Wallet.isMixinSafe(): Boolean {
    return category == WalletCategory.MIXIN_SAFE.value
}
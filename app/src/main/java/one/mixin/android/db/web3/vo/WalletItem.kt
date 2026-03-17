package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.vo.WalletCategory
import java.math.BigDecimal

@Parcelize
data class WalletItem(
    val id: String,
    val category: String,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val safeRole: String?,
    val safeChainId: String?,
    val safeAddress: String?,
    val safeUrl: String?,
) : Parcelable {

    @Ignore
    var hasLocalPrivateKey: Boolean = false

    @Ignore
    var value: BigDecimal = BigDecimal.ZERO

    val safeChain: SafeChain?
        get() = SafeChain.fromValue(safeChainId)
}

fun WalletItem.isTransferFeeFree(): Boolean {
    return category == WalletCategory.CLASSIC.value || (isImported() && hasLocalPrivateKey)
}

fun WalletItem.notClassic(): Boolean {
    return category == WalletCategory.IMPORTED_MNEMONIC.value ||
        category == WalletCategory.IMPORTED_PRIVATE_KEY.value ||
        category == WalletCategory.WATCH_ADDRESS.value ||
        category == WalletCategory.MIXIN_SAFE.value
}

fun WalletItem.isClassic(): Boolean {
    return category == WalletCategory.CLASSIC.value
}

fun WalletItem.isImported(): Boolean {
    return category == WalletCategory.IMPORTED_MNEMONIC.value || category == WalletCategory.IMPORTED_PRIVATE_KEY.value
}

fun WalletItem.isWatch(): Boolean {
    return category == WalletCategory.WATCH_ADDRESS.value
}

fun WalletItem.isMixinSafe(): Boolean {
    return category == WalletCategory.MIXIN_SAFE.value
}

fun WalletItem.isOwner(): Boolean {
    return safeRole.equalsIgnoreCase("owner")
}

fun WalletItem.toWeb3Wallet(): Web3Wallet {
    val id: String = this.id
    val category: String = this.category
    val name: String = this.name
    val createdAt: String = this.createdAt
    val updatedAt: String = this.updatedAt
    val web3Wallet: Web3Wallet = Web3Wallet(
        id = id,
        category = category,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
    web3Wallet.hasLocalPrivateKey = this.hasLocalPrivateKey
    return web3Wallet
}

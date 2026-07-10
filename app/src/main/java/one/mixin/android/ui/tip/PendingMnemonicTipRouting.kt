package one.mixin.android.ui.tip

import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

enum class PendingMnemonicRoute {
    WalletHome,
    ImportMnemonic,
}

sealed class PendingMnemonicResolution {
    data class WalletHome(val walletId: String) : PendingMnemonicResolution()
    object ImportMnemonic : PendingMnemonicResolution()
    object NeedPin : PendingMnemonicResolution()
    object LocalSaveFailed : PendingMnemonicResolution()
}

fun shouldCreateClassicWalletAfterTip(
    tipType: TipType,
): Boolean = tipType == TipType.Create

fun routePendingMnemonicAfterWalletFetch(walletCategories: List<String>?): PendingMnemonicRoute =
    when {
        walletCategories == null -> PendingMnemonicRoute.ImportMnemonic
        walletCategories.any { it == WalletCategory.IMPORTED_MNEMONIC.value } -> PendingMnemonicRoute.WalletHome
        else -> PendingMnemonicRoute.ImportMnemonic
    }

fun importedMnemonicWalletIdForPendingImport(wallets: List<Web3Wallet>?): String? =
    wallets?.firstOrNull { it.category == WalletCategory.IMPORTED_MNEMONIC.value }?.id

suspend fun resolvePendingMnemonicAfterWalletFetch(
    wallets: List<Web3Wallet>?,
    pin: String?,
    pendingWords: List<String>?,
    save: suspend (walletId: String, pin: String, words: List<String>) -> Boolean,
    clear: () -> Unit,
): PendingMnemonicResolution {
    val walletId = importedMnemonicWalletIdForPendingImport(wallets)
        ?: return PendingMnemonicResolution.ImportMnemonic
    val verifiedPin = pin ?: return PendingMnemonicResolution.NeedPin
    val words = pendingWords ?: return PendingMnemonicResolution.LocalSaveFailed
    return runCatching {
        if (!save(walletId, verifiedPin, words)) {
            PendingMnemonicResolution.LocalSaveFailed
        } else {
            clear()
            PendingMnemonicResolution.WalletHome(walletId)
        }
    }.getOrDefault(PendingMnemonicResolution.LocalSaveFailed)
}

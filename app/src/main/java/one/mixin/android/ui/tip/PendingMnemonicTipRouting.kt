package one.mixin.android.ui.tip

import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

enum class PendingMnemonicRoute {
    WalletHome,
    ImportMnemonic,
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

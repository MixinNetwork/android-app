package one.mixin.android.ui.tip

import one.mixin.android.vo.WalletCategory

enum class PendingMnemonicRoute {
    WalletHome,
    ImportMnemonic,
    WalletFetchFailed,
}

fun shouldCreateClassicWalletAfterTip(
    tipType: TipType,
): Boolean = tipType != TipType.Change

fun routePendingMnemonicAfterWalletFetch(walletCategories: List<String>?): PendingMnemonicRoute =
    when {
        walletCategories == null -> PendingMnemonicRoute.WalletFetchFailed
        walletCategories.any { it == WalletCategory.CLASSIC.value } -> PendingMnemonicRoute.WalletHome
        else -> PendingMnemonicRoute.ImportMnemonic
    }

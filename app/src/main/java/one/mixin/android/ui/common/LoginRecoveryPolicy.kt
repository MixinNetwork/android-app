package one.mixin.android.ui.common

import one.mixin.android.vo.WalletCategory

internal fun <T> classicWalletAfterUtxoBackfill(
    refreshedWallets: List<T>,
    backfillSucceeded: Boolean,
): List<T> = refreshedWallets

internal enum class ImportedMnemonicBackfillAction {
    PROCESS,
    SKIP,
}

internal fun importedMnemonicBackfillAction(
    walletCategory: String,
    mnemonic: String?,
): ImportedMnemonicBackfillAction = when {
    walletCategory == WalletCategory.CLASSIC.value -> ImportedMnemonicBackfillAction.PROCESS
    mnemonic != null -> ImportedMnemonicBackfillAction.PROCESS
    else -> ImportedMnemonicBackfillAction.SKIP
}

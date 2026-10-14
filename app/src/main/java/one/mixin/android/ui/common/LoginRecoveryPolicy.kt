package one.mixin.android.ui.common

import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.WalletCategory

internal suspend fun removeMissingWalletForLogin(
    errorCode: Int,
    deleteWallet: suspend () -> Unit,
): Boolean {
    if (errorCode != ErrorHandler.NOT_FOUND) return false
    deleteWallet()
    return true
}

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

package one.mixin.android.ui.tip

import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

sealed class PendingMnemonicResolution {
    data class WalletHome(val walletId: String) : PendingMnemonicResolution()
    object ImportMnemonic : PendingMnemonicResolution()
    object NeedPin : PendingMnemonicResolution()
    object LocalSaveFailed : PendingMnemonicResolution()
}

fun shouldCreateClassicWalletAfterTip(
    tipType: TipType,
): Boolean = tipType == TipType.Create

private suspend fun importedMnemonicWalletIdForPendingImport(
    wallets: List<Web3Wallet>?,
    mnemonicWords: List<String>,
    walletAddresses: suspend (Web3Wallet) -> List<Web3Address>,
): String? {
    val mnemonic = mnemonicWords.joinToString(" ")
    for (wallet in wallets.orEmpty()) {
        if (wallet.category != WalletCategory.IMPORTED_MNEMONIC.value) continue
        val addresses = walletAddresses(wallet)
        if (addresses.isEmpty()) continue
        val matches = addresses.any { address ->
            val derivationIndex = address.path?.let(CryptoWalletHelper::extractIndexFromPath) ?: return@any false
            runCatching {
                CryptoWalletHelper.mnemonicToAddress(
                    mnemonic = mnemonic,
                    chainId = address.chainId,
                    index = derivationIndex,
                ).equals(address.destination, ignoreCase = true)
            }.getOrDefault(false)
        }
        if (matches) return wallet.id
    }
    return null
}

suspend fun resolvePendingMnemonicAfterWalletFetch(
    wallets: List<Web3Wallet>?,
    pin: String?,
    pendingWords: List<String>?,
    walletAddresses: suspend (Web3Wallet) -> List<Web3Address> = { it.addresses.orEmpty() },
    save: suspend (walletId: String, pin: String, words: List<String>) -> Boolean,
    clear: () -> Unit,
): PendingMnemonicResolution {
    if (wallets.isNullOrEmpty() || wallets.none { it.category == WalletCategory.IMPORTED_MNEMONIC.value }) {
        return PendingMnemonicResolution.ImportMnemonic
    }
    val verifiedPin = pin ?: return PendingMnemonicResolution.NeedPin
    val words = pendingWords ?: return PendingMnemonicResolution.LocalSaveFailed
    val walletId = importedMnemonicWalletIdForPendingImport(wallets, words, walletAddresses)
        ?: return PendingMnemonicResolution.ImportMnemonic
    return runCatching {
        if (!save(walletId, verifiedPin, words)) {
            PendingMnemonicResolution.LocalSaveFailed
        } else {
            clear()
            PendingMnemonicResolution.WalletHome(walletId)
        }
    }.getOrDefault(PendingMnemonicResolution.LocalSaveFailed)
}

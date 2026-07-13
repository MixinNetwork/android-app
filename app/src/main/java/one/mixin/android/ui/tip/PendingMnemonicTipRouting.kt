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

fun matchesMnemonicWalletAddresses(
    mnemonic: String,
    addresses: List<Web3Address>,
): Boolean =
    addresses.any { address ->
        val derivationIndex = address.path?.let(CryptoWalletHelper::extractIndexFromPath) ?: return@any false
        runCatching {
            CryptoWalletHelper.mnemonicToAddress(
                mnemonic = mnemonic,
                chainId = address.chainId,
                index = derivationIndex,
            ).equals(address.destination, ignoreCase = true)
        }.getOrDefault(false)
    }

private suspend fun importedMnemonicWalletIdsForPendingImport(
    wallets: List<Web3Wallet>?,
    mnemonicWords: List<String>,
    walletAddresses: suspend (Web3Wallet) -> List<Web3Address>,
): List<String> {
    val mnemonic = mnemonicWords.joinToString(" ")
    return buildList {
        wallets.orEmpty().forEach { wallet ->
            if (wallet.category == WalletCategory.IMPORTED_MNEMONIC.value &&
                matchesMnemonicWalletAddresses(mnemonic, walletAddresses(wallet))
            ) {
                add(wallet.id)
            }
        }
    }
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
    val walletIds = importedMnemonicWalletIdsForPendingImport(wallets, words, walletAddresses)
    if (walletIds.isEmpty()) return PendingMnemonicResolution.ImportMnemonic
    return runCatching {
        if (walletIds.any { walletId -> !save(walletId, verifiedPin, words) }) {
            PendingMnemonicResolution.LocalSaveFailed
        } else {
            clear()
            PendingMnemonicResolution.WalletHome(walletIds.first())
        }
    }.getOrDefault(PendingMnemonicResolution.LocalSaveFailed)
}

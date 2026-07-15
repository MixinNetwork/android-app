package one.mixin.android.ui.tip

import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.vo.WalletCategory

sealed class PendingMnemonicResolution {
    data class WalletHome(
        val walletId: String,
        val walletCategory: String = WalletCategory.IMPORTED_MNEMONIC.value,
    ) : PendingMnemonicResolution()
    object ImportMnemonic : PendingMnemonicResolution()
    object NeedPin : PendingMnemonicResolution()
    object LocalSaveFailed : PendingMnemonicResolution()
}

private data class PendingMnemonicWalletMatch(
    val walletId: String,
    val walletCategory: String,
    val privateKey: String? = null,
)

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

private suspend fun pendingMnemonicWalletMatches(
    wallets: List<Web3Wallet>?,
    mnemonicWords: List<String>,
    walletAddresses: suspend (Web3Wallet) -> List<Web3Address>,
    privateKeyForAddress: (
        mnemonic: String,
        address: Web3Address,
        index: Int,
    ) -> String?,
): List<PendingMnemonicWalletMatch> {
    val mnemonic = mnemonicWords.joinToString(" ")
    return buildList {
        wallets.orEmpty().forEach { wallet ->
            if (wallet.category != WalletCategory.IMPORTED_MNEMONIC.value &&
                wallet.category != WalletCategory.IMPORTED_PRIVATE_KEY.value
            ) {
                return@forEach
            }
            val address = walletAddresses(wallet).firstOrNull { address ->
                val derivationIndex = address.path?.let(CryptoWalletHelper::extractIndexFromPath) ?: return@firstOrNull false
                runCatching {
                    CryptoWalletHelper.mnemonicToAddress(
                        mnemonic = mnemonic,
                        chainId = address.chainId,
                        index = derivationIndex,
                    ).equals(address.destination, ignoreCase = true)
                }.getOrDefault(false)
            } ?: return@forEach
            val privateKey = if (wallet.category == WalletCategory.IMPORTED_PRIVATE_KEY.value) {
                val derivationIndex = address.path?.let(CryptoWalletHelper::extractIndexFromPath)
                    ?: return@forEach
                privateKeyForAddress(mnemonic, address, derivationIndex) ?: return@forEach
            } else {
                null
            }
            add(PendingMnemonicWalletMatch(wallet.id, wallet.category, privateKey))
        }
    }.sortedBy { it.walletCategory != WalletCategory.IMPORTED_PRIVATE_KEY.value }
}

suspend fun resolvePendingMnemonicAfterWalletFetch(
    wallets: List<Web3Wallet>?,
    pin: String?,
    pendingWords: List<String>?,
    walletAddresses: suspend (Web3Wallet) -> List<Web3Address> = { it.addresses.orEmpty() },
    privateKeyForAddress: (
        mnemonic: String,
        address: Web3Address,
        index: Int,
    ) -> String? = { mnemonic, address, index ->
        runCatching { CryptoWalletHelper.mnemonicToImportedPrivateKey(mnemonic, address.chainId, index) }.getOrNull()
    },
    save: suspend (walletId: String, pin: String, words: List<String>) -> Boolean,
    savePrivateKey: suspend (walletId: String, pin: String, privateKey: String) -> Boolean = { _, _, _ -> false },
    clear: () -> Unit,
): PendingMnemonicResolution {
    if (wallets.isNullOrEmpty()) {
        return PendingMnemonicResolution.ImportMnemonic
    }
    val verifiedPin = pin ?: return PendingMnemonicResolution.NeedPin
    val words = pendingWords ?: return PendingMnemonicResolution.LocalSaveFailed
    val matches = pendingMnemonicWalletMatches(wallets, words, walletAddresses, privateKeyForAddress)
    if (matches.isEmpty()) return PendingMnemonicResolution.ImportMnemonic
    return runCatching {
        if (matches.any { match ->
                when (match.walletCategory) {
                    WalletCategory.IMPORTED_MNEMONIC.value -> !save(match.walletId, verifiedPin, words)
                    WalletCategory.IMPORTED_PRIVATE_KEY.value -> !savePrivateKey(
                        match.walletId,
                        verifiedPin,
                        requireNotNull(match.privateKey),
                    )
                    else -> false
                }
            }
        ) {
            PendingMnemonicResolution.LocalSaveFailed
        } else {
            clear()
            PendingMnemonicResolution.WalletHome(matches.first().walletId, matches.first().walletCategory)
        }
    }.getOrDefault(PendingMnemonicResolution.LocalSaveFailed)
}

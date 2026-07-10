package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.toHex
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.time.Instant

const val INITIAL_CLASSIC_WALLET_INDEX = 0

fun classicWalletIndexForCreation(
    hasClassicWallet: Boolean,
    maxClassicIndex: Int,
): Int = if (hasClassicWallet) maxClassicIndex + 1 else INITIAL_CLASSIC_WALLET_INDEX

suspend fun <T> ensureInitialClassicWallet(
    syncWallets: suspend () -> List<T>?,
    isClassicWallet: (T) -> Boolean,
    createClassicWallet: suspend (Int) -> Unit,
): List<T>? {
    val syncedWallets = syncWallets() ?: return null
    if (syncedWallets.any(isClassicWallet)) return syncedWallets
    createClassicWallet(INITIAL_CLASSIC_WALLET_INDEX)
    return syncWallets() ?: syncedWallets
}

suspend fun buildClassicWalletRequest(
    web3Repository: Web3Repository,
    spendKey: ByteArray,
    classicIndex: Int,
): WalletRequest {
    val names = web3Repository.getAllWalletNames(
        listOf(
            WalletCategory.CLASSIC.value,
            WalletCategory.IMPORTED_PRIVATE_KEY.value,
            WalletCategory.IMPORTED_MNEMONIC.value,
        )
    )
    val name = nextCommonWalletName(names)
    val evmAddress = privateKeyToAddress(spendKey, Constants.ChainId.ETHEREUM_CHAIN_ID, classicIndex)
    val solAddress = privateKeyToAddress(spendKey, Constants.ChainId.SOLANA_CHAIN_ID, classicIndex)
    val btcAddress = privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex)
    return WalletRequest(
        name = name,
        category = WalletCategory.CLASSIC.value,
        addresses = listOf(
            createSignedWeb3AddressRequest(
                destination = btcAddress,
                chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                path = Bip44Path.bitcoinSegwitPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value,
            ),
            createSignedWeb3AddressRequest(
                destination = evmAddress,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                path = Bip44Path.ethereumPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.ETHEREUM_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value,
            ),
            createSignedWeb3AddressRequest(
                destination = solAddress,
                chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                path = Bip44Path.solanaPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.SOLANA_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value,
            ),
        )
    )
}

fun nextCommonWalletName(names: List<String?>): String {
    val walletName = MixinApplication.appContext.getString(R.string.Common_Wallet)
    val regex = """^$walletName (\d+)$""".toRegex()
    val maxIndex = names
        .filterNotNull()
        .mapNotNull { name ->
            regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
        }.maxOrNull() ?: 0
    return "$walletName ${maxIndex + 1}"
}

fun createSignedWeb3AddressRequest(
    destination: String,
    chainId: String,
    path: String?,
    privateKey: String,
    category: String,
): Web3AddressRequest {
    val privateKeyBytes = Numeric.hexStringToByteArray(privateKey)
    return createSignedWeb3AddressRequest(destination, chainId, path, privateKeyBytes, category)
}

fun createSignedWeb3AddressRequest(
    destination: String,
    chainId: String,
    path: String?,
    privateKey: ByteArray?,
    category: String,
): Web3AddressRequest {
    val selfId = Session.getAccountId()
    if (category == WalletCategory.WATCH_ADDRESS.value) {
        return Web3AddressRequest(
            destination = destination,
            chainId = chainId,
            path = path,
        )
    }
    val now = Instant.now()
    val signature = if (privateKey != null) {
        val message = "$destination\n$selfId\n${now.epochSecond}"
        when {
            chainId == Constants.ChainId.SOLANA_CHAIN_ID -> {
                Numeric.prependHexPrefix(Web3Signer.signSolanaMessage(privateKey, message.toByteArray()))
            }
            chainId in Constants.Web3EvmChainIds -> {
                Web3Signer.signEthMessage(privateKey, message.toByteArray().toHex(), JsSignMessage.TYPE_PERSONAL_MESSAGE)
            }
            chainId == Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val ecKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
                Numeric.toHexString(ecKey.signMessage(message, ScriptType.P2WPKH).decodeBase64())
            }
            else -> null
        }
    } else {
        null
    }

    return Web3AddressRequest(
        destination = destination,
        chainId = chainId,
        path = path,
        signature = signature,
        timestamp = now.toString(),
    )
}

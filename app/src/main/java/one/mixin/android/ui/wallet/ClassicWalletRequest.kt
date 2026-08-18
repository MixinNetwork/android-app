package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
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
    return syncWallets()?.takeIf { it.any(isClassicWallet) }
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
    return WalletRequest(
        name = name,
        category = WalletCategory.CLASSIC.value,
        addresses = buildList {
            addAll(buildClassicUtxoAddressRequests(spendKey, classicIndex))
            add(createSignedWeb3AddressRequest(
                destination = evmAddress,
                chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                path = Bip44Path.ethereumPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.ETHEREUM_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value,
            ))
            add(createSignedWeb3AddressRequest(
                destination = solAddress,
                chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                path = Bip44Path.solanaPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.SOLANA_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value,
            ))
        }
    )
}

fun buildClassicUtxoAddressRequests(
    spendKey: ByteArray,
    classicIndex: Int,
): List<Web3AddressRequest> = buildList {
    add(
        createSignedWeb3AddressRequest(
            destination = privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex),
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
            path = Bip44Path.bitcoinSegwitPathString(classicIndex),
            privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex),
            category = WalletCategory.CLASSIC.value,
        )
    )
    add(
        createSignedWeb3AddressRequest(
            destination = privateKeyToAddress(spendKey, Constants.ChainId.PEARL_CHAIN_ID, classicIndex),
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            path = Bip44Path.pearlPathString(classicIndex),
            privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.PEARL_CHAIN_ID, classicIndex),
            category = WalletCategory.CLASSIC.value,
        )
    )
}

internal fun validateWalletAddressUpdateResponse(
    walletId: String,
    requestedAddresses: List<Web3AddressRequest>,
    updatedWallet: Web3Wallet,
): List<Web3Address>? {
    if (updatedWallet.id != walletId) return null
    if (requestedAddresses.isEmpty()) return emptyList()

    val requestedChainIds = requestedAddresses.mapTo(linkedSetOf(), Web3AddressRequest::chainId)
    if (requestedChainIds.size != requestedAddresses.size) return null

    val returnedAddresses =
        updatedWallet.addresses
            .orEmpty()
            .filter { address -> address.chainId in requestedChainIds }
    if (returnedAddresses.size != requestedAddresses.size) return null

    return requestedAddresses.map { request ->
        returnedAddresses.singleOrNull { address ->
            address.walletId == walletId &&
                address.chainId == request.chainId &&
                address.destination == request.destination &&
                address.path == request.path
        } ?: return null
    }
}

fun nextWalletNameIndex(
    names: List<String?>,
    walletName: String,
): Int {
    val regex = """^${Regex.escape(walletName)} (\d+)$""".toRegex()
    return names
        .filterNotNull()
        .mapNotNull { name ->
            regex.find(name)?.groupValues?.get(1)?.toIntOrNull()
        }.maxOrNull()?.plus(1) ?: 1
}

fun formatWalletName(walletName: String, index: Int): String = "$walletName $index"

fun nextWalletName(names: List<String?>, walletName: String): String =
    formatWalletName(walletName, nextWalletNameIndex(names, walletName))

fun commonWalletName(index: Int): String =
    formatWalletName(MixinApplication.appContext.getString(R.string.Common_Wallet), index)

fun nextCommonWalletNameIndex(names: List<String?>): Int =
    nextWalletNameIndex(names, MixinApplication.appContext.getString(R.string.Common_Wallet))

fun nextCommonWalletName(names: List<String?>): String =
    commonWalletName(nextCommonWalletNameIndex(names))

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
            chainId == Constants.ChainId.BITCOIN_CHAIN_ID || chainId == Constants.ChainId.PEARL_CHAIN_ID -> {
                signUtxoAddressMessage(privateKey, message, chainId)
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

fun signUtxoAddressMessage(
    privateKey: ByteArray,
    message: String,
    chainId: String,
): String {
    val scriptType = when (chainId) {
        Constants.ChainId.BITCOIN_CHAIN_ID -> ScriptType.P2WPKH
        Constants.ChainId.PEARL_CHAIN_ID -> ScriptType.P2PKH
        else -> throw IllegalArgumentException("Unsupported UTXO chainId: $chainId")
    }
    val ecKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
    return Numeric.toHexString(ecKey.signMessage(message, scriptType).decodeBase64())
}

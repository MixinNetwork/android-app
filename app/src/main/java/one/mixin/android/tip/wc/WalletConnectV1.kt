package one.mixin.android.tip.wc

import android.content.Context
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.WCSessionStoreItem
import com.trustwallet.walletconnect.WCSessionStoreType
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCAddNetwork
import com.trustwallet.walletconnect.models.session.WCSession
import okhttp3.OkHttpClient
import one.mixin.android.MixinApplication
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit

object WalletConnectV1 : WalletConnect() {
    const val TAG = "WalletConnectV1"

    private val wcClient = WCClient(GsonBuilder(), OkHttpClient.Builder().build()).also { wcc ->
        wcc.onSessionRequest = { id, peer ->
            Timber.d("$TAG onSessionRequest id: $id, peer: $peer")
            wcc.session?.let {
                sessionStore.session = WCSessionStoreItem(it, chain.chainReference, "peerId", "remotePeerID", peer)
            }
            onSessionRequest(id, peer)
        }
        wcc.onGetAccounts = { id ->
            Timber.d("$TAG onGetAccounts id: $id")
            onGetAccounts(id)
        }
        wcc.onWalletChangeNetwork = { id, chainId ->
            Timber.d("$TAG onWalletChangeNetwork id: $id")
            val chain = chainId.getChain()
            if (chain == null) {
                rejectRequest(id, "Network not supported")
            } else {
                targetNetwork = chain
                onWalletChangeNetwork(id, chainId)
            }
        }
        wcc.onWalletAddNetwork = { id, network ->
            Timber.d("$TAG onWalletAddNetwork id: $id, network: $network")
            onWalletAddNetwork(id, network)
        }
        wcc.onEthSign = { id, message ->
            Timber.d("$TAG onEthSign id: $id, message: $message")
            currentSignData = WCSignData.V1SignData(id, message)
            onEthSign(id, message)
        }
        wcc.onEthSignTransaction = { id, transaction ->
            Timber.d("$TAG onEthSignTransaction id: $id, transaction: $transaction")
            currentSignData = WCSignData.V1SignData(id, transaction)
            onEthSendTransaction(id, transaction)
        }
        wcc.onEthSendTransaction = { id, transaction ->
            Timber.d("$TAG onEthSendTransaction id: $id, transaction: $transaction")
            currentSignData = WCSignData.V1SignData(id, transaction)
            onEthSendTransaction(id, transaction)
        }
        wcc.onSignTransaction = { id, transaction ->
            Timber.d("$TAG onSignTransaction id: $id, transaction: $transaction")
        }
        wcc.onCustomRequest = { id, payload ->
            Timber.d("$TAG onCustomRequest id: $id, payload: $payload")
        }
        wcc.onDisconnect = { code, reason ->
            Timber.d("$TAG onDisconnect code: $code, reason: $reason")
            disconnect()
        }
        wcc.onFailure = {
            Timber.d("$TAG onFailure ${it.stackTraceToString()}")
        }
    }

    var address: String? = null
    var targetNetwork: Chain? = null

    val sessionStore = WCSessionStoreType(
        MixinApplication.appContext.getSharedPreferences("wallet_connect_v1_session_store", Context.MODE_PRIVATE),
    )

    fun connect(url: String): Boolean {
        disconnect()

        val peerMeta = WCPeerMeta(
            name = "Mixin Messenger",
            url = "https://mixin.one",
            description = "Mixin Messenger Wallet",
        )
        val wcSession = WCSession.from(url) ?: return false

        wcClient.connect(wcSession, peerMeta)
        return true
    }

    fun disconnect() {
        address = null
        targetNetwork = null
        currentSignData = null
        sessionStore.session = null
        if (wcClient.session != null) {
            wcClient.killSession()
        } else {
            wcClient.disconnect()
        }
    }

    fun approveSession(priv: ByteArray) {
        val pub = ECKeyPair.create(priv).publicKey
        val address = Keys.toChecksumAddress(Keys.getAddress(pub))
        Timber.d("$TAG address: $address")
        wcClient.approveSession(listOf(address), chain.chainReference)
        this.address = address
    }

    fun rejectSession() {
        wcClient.rejectSession()
        wcClient.disconnect()
    }

    fun rejectRequest(id: Long, message: String = "Reject by the user") {
        wcClient.rejectRequest(id, message)
        currentSignData = null
    }

    fun walletChangeNetwork(priv: ByteArray, id: Long, chainId: Int = Chain.Polygon.chainReference) {
        val chain = chainId.getChain()
        if (chain == null) {
            rejectRequest(id, "Network not supported")
            return
        }
        this.chain = chain
        val session = sessionStore.session ?: return

        approveSession(priv)

        sessionStore.session = WCSessionStoreItem(session.session, chain.chainReference, session.peerId, session.remotePeerId, session.remotePeerMeta, session.isAutoSign, session.date)
        targetNetwork = null
    }

    fun changeNetwork(chain: Chain) {
        val address = this.address
        if (address == null) {
            Timber.d("$TAG changeNetwork address is null")
            return
        }
        this.chain = chain
        val session = sessionStore.session ?: return

        wcClient.approveSession(listOf(address), chain.chainReference)
        sessionStore.session = WCSessionStoreItem(session.session, chain.chainReference, session.peerId, session.remotePeerId, session.remotePeerMeta, session.isAutoSign, session.date)
    }

    fun getLastSession(): WCSessionStoreItem? = sessionStore.session

    fun ethSignMessage(priv: ByteArray, id: Long, message: WCEthereumSignMessage) {
        wcClient.approveRequest(id, signMessage(priv, message))
    }

    fun ethSignTransaction(priv: ByteArray, id: Long, transaction: WCEthereumTransaction, approve: Boolean): String {
        val value = transaction.value ?: "0x0"
        val maxFeePerGas = transaction.maxFeePerGas?.let { Numeric.toBigInt(it) }
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas?.let { Numeric.toBigInt(it) }
        Timber.d("$TAG ethSignTransaction value: $value, maxFeePerGas: $maxFeePerGas, maxPriorityFeePerGas: $maxPriorityFeePerGas")
//        if (maxFeePerGas == null || maxPriorityFeePerGas == null) {
//            val r = getMaxFeePerGasAndMaxPriorityFeePerGas()
//            if (maxFeePerGas == null) {
//                maxFeePerGas = r.first
//                Timber.d("$TAG ethSignTransaction maxFeePerGas: $maxFeePerGas")
//            }
//            if (maxPriorityFeePerGas == null) {
//                maxPriorityFeePerGas = r.second
//                Timber.d("$TAG ethSignTransaction maxPriorityFeePerGas: $maxPriorityFeePerGas")
//            }
//        }

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val transactionCount = web3j.ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get(web3jTimeout, TimeUnit.SECONDS)
        if (transactionCount.hasError()) {
            throwError(transactionCount.error)
        }
        val nonce = transactionCount.transactionCount
        val v = Numeric.toBigInt(value)
        val gasLimit = transaction.gasLimit?.let { Numeric.toBigInt(it) } ?: BigInteger(defaultGasLimit)
        Timber.d("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit")
        val rawTransaction = if (maxFeePerGas == null && maxPriorityFeePerGas == null) {
            val gasPrice = if (transaction.gasPrice != null) {
                Numeric.toBigInt(transaction.gasPrice)
            } else {
                val ethGasPrice =
                    web3j.ethGasPrice().sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
                if (ethGasPrice.hasError()) {
                    throwError(ethGasPrice.error)
                }
                ethGasPrice.gasPrice
            }
            Timber.d("$TAG gasPrice $gasPrice")
            RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                transaction.to,
                v,
                transaction.data,
            )
        } else {
            RawTransaction.createTransaction(
                chain.chainReference.toLong(),
                nonce,
                gasLimit,
                transaction.to,
                v,
                transaction.data,
                maxPriorityFeePerGas,
                maxFeePerGas,
            )
        }
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction hexMessage: $hexMessage")
        if (approve) {
            wcClient.approveRequest(id, hexMessage)
        }
        return hexMessage
    }

    fun ethSendTransaction(priv: ByteArray, id: Long, transaction: WCEthereumTransaction) {
        val hexMessage = ethSignTransaction(priv, id, transaction, false)
        val raw = web3j.ethSendRawTransaction(hexMessage).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        val transactionHash = raw.transactionHash
        if (raw.hasError()) {
            throwError(raw.error) {
                wcClient.rejectRequest(id, it)
            }
        } else {
            Timber.d("$TAG sendTransaction $transactionHash")
            wcClient.approveRequest(id, transactionHash)
        }
    }

    var onSessionRequest: (id: Long, peer: WCPeerMeta) -> Unit = { _, _ -> Unit }
    var onEthSign: (id: Long, message: WCEthereumSignMessage) -> Unit = { _, _ -> Unit }
    var onEthSignTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> Unit }
    var onEthSendTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> Unit }
    var onCustomRequest: (id: Long, payload: String) -> Unit = { _, _ -> Unit }
    var onGetAccounts: (id: Long) -> Unit = { _ -> Unit }
    var onWalletChangeNetwork: (id: Long, chainId: Int) -> Unit = { _, _ -> Unit }
    var onWalletAddNetwork: (id: Long, network: WCAddNetwork) -> Unit = { _, _ -> Unit }
}

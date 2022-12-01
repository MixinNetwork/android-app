package one.mixin.android.tip.wc

import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.WCSignTransaction
import com.trustwallet.walletconnect.models.binance.WCBinanceCancelOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTradeOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTransferOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTxConfirmParam
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCAddNetwork
import com.trustwallet.walletconnect.models.session.WCSession
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import one.mixin.android.MixinApplication
import one.mixin.android.extension.runOnUiThread
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger

class WalletConnect private constructor() {
    companion object {
        const val TAG = "WalletConnect"

        @Synchronized
        fun get(): WalletConnect {
            if (instance == null) {
                instance = WalletConnect()
            }
            return instance as WalletConnect
        }

        private var instance: WalletConnect? = null

        fun hasInit() = instance != null

        fun release() {
            instance?.disconnect()
            instance = null
        }
    }

    private val wcClient = WCClient(GsonBuilder(), OkHttpClient.Builder().build()).also { wcc ->
        wcc.onSessionRequest = { id, peer ->
            Timber.d("$TAG onSessionRequest id: $id, peer: $peer")
            remotePeerMeta = peer
            onSessionRequest(id, peer)
        }
        wcc.onGetAccounts = { id ->
            Timber.d("$TAG onGetAccounts id: $id")
            onGetAccounts(id)
        }
        wcc.onWalletChangeNetwork = { id, chainId ->
            Timber.d("$TAG onWalletChangeNetwork id: $id")
            onWalletChangeNetwork(id, chainId)
        }
        wcc.onWalletAddNetwork = { id, network ->
            Timber.d("$TAG onWalletAddNetwork id: $id, network: $network")
            onWalletAddNetwork(id, network)
        }
        wcc.onEthSign = { id, message ->
            Timber.d("$TAG onEthSign id: $id, message: $message")
            onEthSign(id, message)
        }
        wcc.onEthSignTransaction = { id, transaction ->
            Timber.d("$TAG onEthSignTransaction id: $id, transaction: $transaction")
            onEthSendTransaction(id, transaction)
        }
        wcc.onEthSendTransaction = { id, transaction ->
            Timber.d("$TAG onEthSendTransaction id: $id, transaction: $transaction")
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

        wcc.addSocketListener(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                MixinApplication.appContext.runOnUiThread {
                    walletConnectLiveData.connected = true
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                MixinApplication.appContext.runOnUiThread {
                    walletConnectLiveData.connected = false
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                MixinApplication.appContext.runOnUiThread {
                    walletConnectLiveData.connected = false
                }
            }
        })
    }

    var remotePeerMeta: WCPeerMeta? = null
    var balance: BigInteger? = null
    var address: String? = null

    fun connect(url: String): Boolean {
        disconnect()

        val peerMeta = WCPeerMeta(
            name = "Mixin Messenger",
            url = "https://mixin.one",
            description = "Mixin Messenger Wallet"
        )
        val wcSession = WCSession.from(url) ?: return false

        wcClient.connect(wcSession, peerMeta)
        return true
    }

    fun disconnect() {
        remotePeerMeta = null
        balance = null
        address = null
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
        wcClient.approveSession(listOf(address), Chain.Polygon.chainId)

        val web3j = Web3j.build(HttpService(Chain.Polygon.rpcServers[0]))
        balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get().balance
        this.address = address
    }

    fun rejectSession() {
        wcClient.rejectSession()
        wcClient.disconnect()
    }

    fun rejectRequest(id: Long) {
        wcClient.rejectRequest(id, "Reject by the user")
    }

    fun getNetworkName(): String? {
        val chainId = wcClient.chainId ?: return null

        return when (chainId) {
            Chain.Ethereum.chainId.toString() -> Chain.Ethereum.name
            Chain.Polygon.chainId.toString() -> Chain.Polygon.name
            else -> null
        }
    }

    fun getBalanceString(): String? {
        val balance = this.balance ?: return null
        val chainId = wcClient.chainId ?: return null

        return when (chainId) {
            Chain.Ethereum.chainId.toString() -> "$balance ${Chain.Ethereum.symbol}"
            Chain.Polygon.chainId.toString() -> "$balance ${Chain.Polygon.symbol}"
            else -> null
        }
    }

    fun signMessage(priv: ByteArray, id: Long, message: ByteArray) {
        val keyPair = ECKeyPair.create(priv)
        val signature = Sign.signPrefixedMessage(message, keyPair)
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        wcClient.approveRequest(id, Numeric.toHexString(b))
    }

    fun sendTransaction(priv: ByteArray, id: Long, transaction: WCEthereumTransaction) {
        val value = transaction.value
        val gasLimit = transaction.gasLimit
        val maxFeePerGas = transaction.maxFeePerGas
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas
        if (value == null || gasLimit == null || maxFeePerGas == null || maxPriorityFeePerGas == null) {
            Timber.d("$TAG value: $value, gasLimit: $gasLimit, maxFeePerGas: $maxFeePerGas, maxPriorityFeePerGas: $maxPriorityFeePerGas")
            return
        }

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)

        val web3j = Web3j.build(HttpService(Chain.Polygon.rpcServers[0]))
        val transactionCount = web3j.ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST).sendAsync().get()
        val nonce = transactionCount.transactionCount
        val v = Numeric.toBigInt(value)
        Timber.d("$TAG nonce: $nonce, value $v wei")
        val rawTransaction = RawTransaction.createEtherTransaction(
            Chain.Polygon.chainId.toLong(),
            nonce,
            Numeric.toBigInt(gasLimit),
            transaction.to,
            v,
            Numeric.toBigInt(maxPriorityFeePerGas),
            Numeric.toBigInt(maxFeePerGas)
        )
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG sendTransaction $hexMessage")
        wcClient.approveRequest(id, hexMessage)
    }

    var onSessionRequest: (id: Long, peer: WCPeerMeta) -> Unit = { _, _ -> Unit }
    var onEthSign: (id: Long, message: WCEthereumSignMessage) -> Unit = { _, _ -> Unit }
    var onEthSignTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> Unit }
    var onEthSendTransaction: (id: Long, transaction: WCEthereumTransaction) -> Unit = { _, _ -> Unit }
    var onCustomRequest: (id: Long, payload: String) -> Unit = { _, _ -> Unit }
    var onBnbTrade: (id: Long, order: WCBinanceTradeOrder) -> Unit = { _, _ -> Unit }
    var onBnbCancel: (id: Long, order: WCBinanceCancelOrder) -> Unit = { _, _ -> Unit }
    var onBnbTransfer: (id: Long, order: WCBinanceTransferOrder) -> Unit = { _, _ -> Unit }
    var onBnbTxConfirm: (id: Long, order: WCBinanceTxConfirmParam) -> Unit = { _, _ -> Unit }
    var onGetAccounts: (id: Long) -> Unit = { _ -> Unit }
    var onSignTransaction: (id: Long, transaction: WCSignTransaction) -> Unit = { _, _ -> Unit }
    var onWalletChangeNetwork: (id: Long, chainId: Int) -> Unit = { _, _ -> Unit }
    var onWalletAddNetwork: (id: Long, network: WCAddNetwork) -> Unit = { _, _ -> Unit }
}

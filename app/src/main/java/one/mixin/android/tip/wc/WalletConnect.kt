package one.mixin.android.tip.wc

import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
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
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class WalletConnect private constructor() {
    companion object {
        const val TAG = "WalletConnect"

        private const val web3jTimeout = 3L
        private const val defaultGasLimit = "250000"

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
            val chain = chainId.getChain()
            if (chain == null) {
                rejectRequest(id, "Network not supported")
            } else {
                onWalletChangeNetwork(id, chainId)
            }
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
            currentWCEthereumTransaction = transaction
            onEthSendTransaction(id, transaction)
        }
        wcc.onEthSendTransaction = { id, transaction ->
            Timber.d("$TAG onEthSendTransaction id: $id, transaction: $transaction")
            currentWCEthereumTransaction = transaction
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
    var balance: String? = null
    var address: String? = null
    var currentWCEthereumTransaction: WCEthereumTransaction? = null

    var chain: Chain = Chain.Polygon
        private set
    private var web3j = Web3j.build(HttpService(chain.rpcServers[0]))

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
        currentWCEthereumTransaction = null
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
        wcClient.approveSession(listOf(address), chain.chainId)

        web3j = Web3j.build(HttpService(chain.rpcServers[0]))
        val balanceResp = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get(web3jTimeout, TimeUnit.SECONDS)
        val balance = balanceResp.balance
        if (balance == null) {
            val msg = "code: ${balanceResp.error.code}, message: ${balanceResp.error.message}"
            Timber.d("$TAG access balance error: $msg")
            this.balance = null
        } else {
            this.balance = Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toPlainString()
        }
        this.address = address
    }

    fun rejectSession() {
        wcClient.rejectSession()
        wcClient.disconnect()
        currentWCEthereumTransaction = null
    }

    fun rejectRequest(id: Long, message: String = "Reject by the user") {
        wcClient.rejectRequest(id, message)
        currentWCEthereumTransaction = null
    }

    fun walletChangeNetwork(priv: ByteArray, id: Long, chainId: Int = Chain.Polygon.chainId) {
        val chain = chainId.getChain()
        if (chain == null) {
            rejectRequest(id, "Network not supported")
            return
        }
        this.chain = chain
        approveSession(priv)
    }

    fun getNetworkName(): String? {
        val chainId = wcClient.chainId ?: return null

        return chainId.getChainName()
    }

    fun getBalanceString(): String? {
        val balance = this.balance ?: return null
        val chainId = wcClient.chainId ?: return null

        return "$balance ${chainId.getChainSymbol()}"
    }

    fun ethSignMessage(priv: ByteArray, id: Long, message: ByteArray) {
        val keyPair = ECKeyPair.create(priv)
        val signature = Sign.signPrefixedMessage(message, keyPair)
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        wcClient.approveRequest(id, Numeric.toHexString(b))
    }

    fun ethSignTransaction(priv: ByteArray, id: Long, transaction: WCEthereumTransaction, approve: Boolean, web3: Web3j? = null): String {
        val value = transaction.value
        val maxFeePerGas = transaction.maxFeePerGas
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas
        if (value == null || maxFeePerGas == null || maxPriorityFeePerGas == null) {
            val msg = "value: $value maxFeePerGas: $maxFeePerGas, maxPriorityFeePerGas: $maxPriorityFeePerGas"
            Timber.d("$TAG $msg")
            wcClient.rejectRequest(id, msg)
            throw WalletConnectException(-1, msg)
        }
        val gasLimit = transaction.gasLimit ?: defaultGasLimit

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val transactionCount = web3j.ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST)
            .sendAsync()
            .get(web3jTimeout, TimeUnit.SECONDS)
        val nonce = transactionCount.transactionCount
        val v = Numeric.toBigInt(value)
        Timber.d("$TAG nonce: $nonce, value $v wei")
        val rawTransaction = RawTransaction.createTransaction(
            chain.chainId.toLong(),
            nonce,
            Numeric.toBigInt(gasLimit),
            transaction.to,
            v,
            transaction.data,
            Numeric.toBigInt(maxPriorityFeePerGas),
            Numeric.toBigInt(maxFeePerGas)
        )
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainId.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        if (approve) {
            wcClient.approveRequest(id, hexMessage)
            currentWCEthereumTransaction = null
        }
        return hexMessage
    }

    fun ethSendTransaction(priv: ByteArray, id: Long, transaction: WCEthereumTransaction) {
        val hexMessage = ethSignTransaction(priv, id, transaction, false, web3j)
        val raw = web3j.ethSendRawTransaction(hexMessage).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        val transactionHash = raw.transactionHash
        if (transactionHash == null) {
            val msg = "error code: ${raw.error.code}, message: ${raw.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            wcClient.rejectRequest(id, msg)
            throw WalletConnectException(raw.error.code, raw.error.message)
        } else {
            Timber.d("$TAG sendTransaction $transactionHash")
            wcClient.approveRequest(id, transactionHash)
        }
        currentWCEthereumTransaction = null
    }

    fun getHumanReadableTransactionInfo(wct: WCEthereumTransaction): String {
        val result = StringBuilder()
        val estimateGas = getEstimateGas(wct)
        val amount = Numeric.toBigInt(wct.value)
        return result.append("Estimated gas fee: ${Convert.fromWei(estimateGas.toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
            .append("Amount + gas fee: ${Convert.fromWei((estimateGas + amount).toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
            .append("maxFeePerGas: ${Convert.fromWei(Numeric.toBigInt(wct.maxFeePerGas).toBigDecimal(), Convert.Unit.GWEI).toPlainString()} GWEI\n")
            .append("maxPriorityFeePerGas: ${Convert.fromWei(Numeric.toBigInt(wct.maxPriorityFeePerGas).toBigDecimal(), Convert.Unit.GWEI).toPlainString()} GWEI\n\n")
            .append("HEX Data\n")
            .append(wct.data)
            .toString()
    }

    private fun getEstimateGas(wct: WCEthereumTransaction): BigInteger {
        val gasPrice = if (wct.maxFeePerGas != null) {
            Numeric.toBigInt(wct.maxFeePerGas)
        } else {
            val ethGasPrice = web3j.ethGasPrice().sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
            if (ethGasPrice.hasError()) {
                val error = ethGasPrice.error
                val msg = "error code: ${error.code}, message: ${error.message}"
                Timber.d("$TAG ethGasPrice error $msg")
                throw WalletConnectException(error.code, error.message)
            }
            ethGasPrice.gasPrice
        }
        val gas = if (wct.gas != null) {
            Numeric.toBigInt(wct.gas)
        } else {
            val tx = Transaction.createFunctionCallTransaction(
                wct.from,
                null,
                gasPrice,
                Numeric.toBigInt(wct.gasLimit ?: defaultGasLimit),
                wct.to,
                Numeric.toBigInt(wct.value),
                wct.data
            )
            val ethEstimateGas =
                web3j.ethEstimateGas(tx).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
            if (ethEstimateGas.hasError()) {
                val error = ethEstimateGas.error
                val msg = "error code: ${error.code}, message: ${error.message}"
                Timber.d("$TAG ethEstimateGas error $msg")
                throw WalletConnectException(error.code, error.message)
            }
            ethEstimateGas.amountUsed
        }
        return gas * gasPrice
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

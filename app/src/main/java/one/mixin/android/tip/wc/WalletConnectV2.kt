package one.mixin.android.tip.wc

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.cacao.sign
import com.walletconnect.android.cacao.signature.SignatureType
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import com.walletconnect.web3.wallet.utils.CacaoSigner
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.tip.wc.eth.WCEthereumSignMessage
import one.mixin.android.tip.wc.eth.WCEthereumTransaction
import one.mixin.android.tip.wc.eth.ethTransactionSerializer
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

object WalletConnectV2 : WalletConnect() {
    const val TAG = "WalletConnectV2"

    var authRequest: Wallet.Model.AuthRequest? = null
    var sessionRequest: Wallet.Model.SessionRequest? = null
    var currentWCEthereumTransaction: WCEthereumTransaction? = null

    var chain: Chain = Chain.Polygon
        private set
    private var web3j = Web3j.build(HttpService(chain.rpcServers[0]))

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(ethTransactionSerializer)
        .create()

    init {
        val projectId = BuildConfig.WC_PROJECT_ID
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val appMetaData = Core.Model.AppMetaData(
            name = "Mixin Wallet",
            url = "https://mixin.one",
            description = "Mixin Wallet",
            icons = emptyList(),
            redirect = null,
        )
        CoreClient.initialize(
            relayServerUrl = serverUrl,
            connectionType = ConnectionType.AUTOMATIC,
            application = MixinApplication.get(),
            metaData = appMetaData,
            onError = { error ->
                Timber.d("$TAG CoreClient init error: $error")
            },
        )
        val initParams = Wallet.Params.Init(core = CoreClient)
        Web3Wallet.initialize(initParams) { error ->
            Timber.d("$TAG Web3Wallet init error: $error")
        }

        val coreDelegate = object : CoreClient.CoreDelegate {
            override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
                Timber.d("$TAG onPairingDelete $deletedPairing")
                this@WalletConnectV2.onPairingDelete(deletedPairing)
            }
        }

        val walletDelegate = object : Web3Wallet.WalletDelegate {
            override fun onAuthRequest(authRequest: Wallet.Model.AuthRequest) {
                Timber.d("$TAG onAuthRequest $authRequest")
                this@WalletConnectV2.authRequest = authRequest
                this@WalletConnectV2.onAuthRequest(authRequest)
            }

            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                Timber.d("$TAG onConnectionStateChange $state")
                this@WalletConnectV2.onConnectionStateChange(state)
            }

            override fun onError(error: Wallet.Model.Error) {
                Timber.d("$TAG onError $error")
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                Timber.d("$TAG onSessionDelete $sessionDelete")
                this@WalletConnectV2.onSessionDelete(sessionDelete)
            }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
                Timber.d("$TAG onSessionProposal $sessionProposal")
                this@WalletConnectV2.onSessionProposal(sessionProposal)
            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                Timber.d("$TAG onSessionRequest $sessionRequest")
                this@WalletConnectV2.sessionRequest = sessionRequest
                this@WalletConnectV2.onSessionRequest(sessionRequest)
            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                Timber.d("$TAG onSessionSettleResponse $settleSessionResponse")
                this@WalletConnectV2.onSessionSettleResponse(settleSessionResponse)
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                Timber.d("$TAG onSessionUpdateResponse $sessionUpdateResponse")
                this@WalletConnectV2.onSessionUpdateResponse(sessionUpdateResponse)
            }
        }

        CoreClient.setDelegate(coreDelegate)
        Web3Wallet.setWalletDelegate(walletDelegate)
    }

    fun pair(uri: String) {
        val pairParams = Wallet.Params.Pair(uri)
        Web3Wallet.pair(pairParams, {
            Timber.d("$TAG pair success")
        }) { error ->
            Timber.d("$TAG pair $uri, error: $error")
        }
    }

    fun approveSession(priv: ByteArray) {
        val sessionProposal = Web3Wallet.getSessionProposals().lastOrNull() ?: return

        val pub = ECKeyPair.create(priv).publicKey
        val address = Keys.toChecksumAddress(Keys.getAddress(pub))

        // hardcode
        val chains = listOf(Chain.Ethereum.chainId, Chain.Polygon.chainId)
        val supportAccounts = listOf(
            Chain.Ethereum to address,
            Chain.Polygon to address,
        )

        val selectedAccounts: Map<Chain, String> = chains.map { namespaceChainId ->
            supportAccounts.firstOrNull { (chain, address) -> chain.chainId == namespaceChainId }
        }.filterNotNull().toMap()

        val sessionNamespacesIndexedByNamespace: Map<String, Wallet.Model.Namespace.Session> =
            selectedAccounts.filter { (chain: Chain, _) ->
                sessionProposal.requiredNamespaces
                    .filter { (_, namespace) -> namespace.chains != null }
                    .flatMap { (_, namespace) -> namespace.chains!! }
                    .contains(chain.chainId)
            }.toList()
                .groupBy { (chain: Chain, _: String) -> chain.chainNamespace }
                .asIterable()
                .associate { (key: String, chainData: List<Pair<Chain, String>>) ->
                    val accounts = chainData.map { (chain: Chain, accountAddress: String) ->
                        "${chain.chainNamespace}:${chain.chainReference}:$accountAddress"
                    }

                    val methods = sessionProposal.requiredNamespaces.values
                        .filter { namespace -> namespace.chains != null }
                        .flatMap { it.methods }

                    val events = sessionProposal.requiredNamespaces.values
                        .filter { namespace -> namespace.chains != null }
                        .flatMap { it.events }

                    val chains: List<String> =
                        sessionProposal.requiredNamespaces.values
                            .filter { namespace -> namespace.chains != null }
                            .flatMap { namespace -> namespace.chains!! }

                    key to Wallet.Model.Namespace.Session(
                        accounts = accounts,
                        methods = methods,
                        events = events,
                        chains = chains.ifEmpty { null },
                    )
                }

        val sessionNamespacesIndexedByChain: Map<String, Wallet.Model.Namespace.Session> =
            selectedAccounts.filter { (chain: Chain, _) ->
                sessionProposal.requiredNamespaces
                    .filter { (namespaceKey, namespace) -> namespace.chains == null && namespaceKey == chain.chainId }
                    .isNotEmpty()
            }.toList()
                .groupBy { (chain: Chain, _: String) -> chain.chainId }
                .asIterable()
                .associate { (key: String, chainData: List<Pair<Chain, String>>) ->
                    val accounts = chainData.map { (chain: Chain, accountAddress: String) ->
                        "${chain.chainNamespace}:${chain.chainReference}:$accountAddress"
                    }

                    val methods = sessionProposal.requiredNamespaces.values
                        .filter { namespace -> namespace.chains == null }
                        .flatMap { it.methods }

                    val events = sessionProposal.requiredNamespaces.values
                        .filter { namespace -> namespace.chains == null }
                        .flatMap { it.events }

                    key to Wallet.Model.Namespace.Session(
                        accounts = accounts,
                        methods = methods,
                        events = events,
                    )
                }

        val sessionNamespaces = sessionNamespacesIndexedByNamespace.plus(sessionNamespacesIndexedByChain)

        val approveParams: Wallet.Params.SessionApprove = Wallet.Params.SessionApprove(
            sessionProposal.proposerPublicKey,
            sessionNamespaces,
        )
        Web3Wallet.approveSession(approveParams) { error ->
            Timber.d("$TAG approveSession error: $error")
        }
    }

    fun rejectSession() {
        val sessionProposal = Web3Wallet.getSessionProposals().lastOrNull() ?: return

        val rejectParams: Wallet.Params.SessionReject = Wallet.Params.SessionReject(
            sessionProposal.proposerPublicKey,
            "Reject session",
        )
        Web3Wallet.rejectSession(rejectParams) { error ->
            Timber.d("$TAG rejectSession error: $error")
        }
    }

    fun approveAuthRequest(priv: ByteArray) {
        val request = this.authRequest ?: return

        val pub = ECKeyPair.create(priv).publicKey
        val address = Keys.toChecksumAddress(Keys.getAddress(pub))
        val issuer = Pair(Chain.Polygon, address).toIssuer()
        val message = Web3Wallet.formatMessage(Wallet.Params.FormatMessage(request.payloadParams, issuer)) ?: throw Exception("Error formatting message")

        Web3Wallet.respondAuthRequest(
            Wallet.Params.AuthRequestResponse.Result(
                id = request.id,
                signature = CacaoSigner.sign(message, priv, SignatureType.EIP191),
                issuer = issuer,
            ),
        ) { error ->
            Timber.d("$TAG respondAuthRequest error $error")
        }
        this.authRequest = null
    }

    fun rejectAuthRequest() {
        val request = this.authRequest ?: return

        Web3Wallet.respondAuthRequest(
            Wallet.Params.AuthRequestResponse.Error(
                request.id,
                12001,
                "User Rejected Request",
            ),
        ) { error ->
            Timber.d("$TAG rejectAuthRequest $error")
        }
        this.authRequest = null
    }

    fun approveRequest(priv: ByteArray) {
        val request = this.sessionRequest ?: return

        Timber.d("$TAG ${request.request.params}")
        when (request.request.method) {
            Method.ETHSign.name -> {
                val data = JsonParser.parseString(request.request.params).asJsonArray[1].toString().trim('"')
                Timber.d("$TAG eth sign: $data")
                ethSignData(priv, request.request.id, request.topic, Numeric.hexStringToByteArray(data))
            }
            Method.ETHPersonalSign.name -> {
                val data = JsonParser.parseString(request.request.params).asJsonArray[0].toString().trim('"')
                Timber.d("$TAG personal sign: $data")
                ethSignData(priv, request.request.id, request.topic, Numeric.hexStringToByteArray(data))
            }
            Method.ETHSignTypedData.name, Method.ETHSignTypedDataV4.name -> {
                val data = JsonParser.parseString(request.request.params).asJsonArray[1].toString()
                Timber.d("$TAG sign typed data: $data")
                ethSignData(priv, request.request.id, request.topic, data.toByteArray())
            }
            Method.ETHSignTransaction.name -> {
                val params = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull() ?: return
                this.currentWCEthereumTransaction = params
                ethSignTransaction(priv, request.request.id, request.topic, params, true)
            }
            Method.ETHSendTransaction.name -> {
                val params = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull() ?: return
                this.currentWCEthereumTransaction = params
                ethSendTransaction(priv, request.request.id, request.topic, params)
            }
        }
    }

    fun rejectRequest(message: String? = null) {
        val request = this.sessionRequest ?: return

        val result = Wallet.Params.SessionRequestResponse(
            sessionTopic = request.topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcError(
                id = request.request.id,
                code = 500,
                message = message ?: "Mixin Wallet Error",
            ),
        )
        Web3Wallet.respondSessionRequest(result) { error ->
            Timber.d("$TAG rejectSessionRequest error: $error")
        }

        Web3Wallet.getActiveSessionByTopic(request.topic)?.redirect?.toUri()
            ?.let { deepLinkUri -> sendResponseDeepLink(deepLinkUri) }
        this.sessionRequest = null
        this.currentWCEthereumTransaction = null
    }

    fun getHumanReadableTransactionInfo(wct: WCEthereumTransaction): String {
        val result = StringBuilder()
        val estimateGas = getEstimateGas(wct)
        val amount = Numeric.toBigInt(wct.value)
        result.append("Estimated gas fee: ${Convert.fromWei(estimateGas.toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
            .append("Amount + gas fee: ${Convert.fromWei((estimateGas + amount).toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
        wct.maxFeePerGas?.let { result.append("maxFeePerGas: ${Convert.fromWei(Numeric.toBigInt(it).toBigDecimal(), Convert.Unit.GWEI).toPlainString()} GWEI\n") }
        wct.maxPriorityFeePerGas?.let { result.append("maxPriorityFeePerGas: ${Convert.fromWei(Numeric.toBigInt(it).toBigDecimal(), Convert.Unit.GWEI).toPlainString()} GWEI\n\n") }
        return result.append("HEX Data\n")
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
                wct.data,
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

    fun getListOfActiveSessions(): List<Wallet.Model.Session> {
        return try {
            Web3Wallet.getListOfActiveSessions()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getListOfActiveSessions ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun getActiveSessionByTopic(topic: String): Wallet.Model.Session? {
        return try {
            Web3Wallet.getActiveSessionByTopic(topic)
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getActiveSessionByTopic ${e.stackTraceToString()}")
            null
        }
    }

    fun getSessionProposals(): List<Wallet.Model.SessionProposal> {
        return try {
            Web3Wallet.getSessionProposals()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getSessionProposals ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun disconnect(topic: String) {
        Web3Wallet.disconnectSession(
            Wallet.Params.SessionDisconnect(topic),
            onSuccess = {
                Timber.d("$TAG disconnect success")
            },
        ) { error ->
            Timber.d("$TAG disconnect error $error")
        }
    }

    private fun approveRequestInternal(result: String, topic: String, requestId: Long) {
        Timber.d("$TAG approve request $result")
        val response = Wallet.Params.SessionRequestResponse(
            sessionTopic = topic,
            jsonRpcResponse = Wallet.Model.JsonRpcResponse.JsonRpcResult(
                requestId,
                result,
            ),
        )
        Web3Wallet.respondSessionRequest(response) { error ->
            Timber.d("$TAG approveSessionRequest error: $error")
        }

        Web3Wallet.getActiveSessionByTopic(topic)?.redirect?.toUri()
            ?.let { deepLinkUri -> sendResponseDeepLink(deepLinkUri) }
//        this.sessionRequest = null
    }

    private fun ethSignData(priv: ByteArray, id: Long, topic: String, data: ByteArray) {
        val keyPair = ECKeyPair.create(priv)
        val signature = Sign.signPrefixedMessage(data, keyPair)
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        approveRequestInternal(Numeric.toHexString(b), topic, id)
    }

    private fun ethSign(priv: ByteArray, id: Long, topic: String, message: WCEthereumSignMessage) {
        val keyPair = ECKeyPair.create(priv)
        val signature = Sign.signPrefixedMessage(message.data.toByteArray(), keyPair)
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        approveRequestInternal(Numeric.toHexString(b), topic, id)
    }

    private fun ethSignTransaction(priv: ByteArray, id: Long, topic: String, transaction: WCEthereumTransaction, approve: Boolean): String {
        val value = transaction.value
        val maxFeePerGas = transaction.maxFeePerGas
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas
        if (value == null || maxFeePerGas == null || maxPriorityFeePerGas == null) {
            val msg = "value: $value maxFeePerGas: $maxFeePerGas, maxPriorityFeePerGas: $maxPriorityFeePerGas"
            Timber.d("$TAG $msg")
            rejectRequest(msg)
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
            chain.chainReference.toLong(),
            nonce,
            Numeric.toBigInt(gasLimit),
            transaction.to,
            v,
            transaction.data,
            Numeric.toBigInt(maxPriorityFeePerGas),
            Numeric.toBigInt(maxFeePerGas),
        )
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        if (approve) {
            approveRequestInternal(hexMessage, topic, id)
        }
        return hexMessage
    }

    private fun ethSendTransaction(priv: ByteArray, id: Long, topic: String, transaction: WCEthereumTransaction) {
        val hexMessage = ethSignTransaction(priv, id, topic, transaction, false)
        val raw = web3j.ethSendRawTransaction(hexMessage).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        val transactionHash = raw.transactionHash
        if (transactionHash == null) {
            val msg = "error code: ${raw.error.code}, message: ${raw.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            rejectRequest(msg)
            throw WalletConnectException(raw.error.code, raw.error.message)
        } else {
            Timber.d("$TAG sendTransaction $transactionHash")
            approveRequestInternal(transactionHash, topic, id)
        }
    }

    private fun sendResponseDeepLink(sessionRequestDeeplinkUri: Uri) {
        try {
            MixinApplication.appContext.startActivity(Intent(Intent.ACTION_VIEW, sessionRequestDeeplinkUri))
        } catch (exception: ActivityNotFoundException) {
            // There is no app to handle deep link
            Timber.d("$TAG sendResponseDeepLink meet ActivityNotFoundException")
        }
    }

    var onAuthRequest: (authRequest: Wallet.Model.AuthRequest) -> Unit = { _ -> }
    var onConnectionStateChange: (state: Wallet.Model.ConnectionState) -> Unit = { _ -> }
    var onSessionDelete: (sessionDelete: Wallet.Model.SessionDelete) -> Unit = { _ -> }
    var onSessionProposal: (sessionProposal: Wallet.Model.SessionProposal) -> Unit = { _ -> }
    var onSessionRequest: (sessionRequest: Wallet.Model.SessionRequest) -> Unit = { _ -> }
    var onSessionSettleResponse: (settleSessionResponse: Wallet.Model.SettledSessionResponse) -> Unit = { _ -> }
    var onSessionUpdateResponse: (sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) -> Unit = { _ -> }
    var onPairingDelete: (deletedPairing: Core.Model.DeletedPairing) -> Unit = { _ -> }
}

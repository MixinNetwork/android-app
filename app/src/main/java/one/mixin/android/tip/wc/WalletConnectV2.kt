package one.mixin.android.tip.wc

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.ethereum.ethTransactionSerializer
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit

object WalletConnectV2 : WalletConnect() {
    const val TAG = "WalletConnectV2"

    var authRequest: Wallet.Model.AuthRequest? = null
    var signedTransactionData: String? = null

    private val gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(ethTransactionSerializer)
        .create()

    init {
        val projectId = BuildConfig.WC_PROJECT_ID
        val relayUrl = "relay.walletconnect.com"
        val serverUrl = "wss://$relayUrl?projectId=$projectId"
        val appMetaData = Core.Model.AppMetaData(
            name = "Mixin Messenger",
            url = "https://messenger.mixin.one",
            description = "An open source cryptocurrency wallet with Signal messaging. Fully non-custodial and recoverable with phone number and TIP.",
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
                RxBus.publish(WCErrorEvent(WCError(error.throwable)))
            },
        )
        val initParams = Wallet.Params.Init(core = CoreClient)
        Web3Wallet.initialize(initParams) { error ->
            Timber.d("$TAG Web3Wallet init error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
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
                RxBus.publish(WCErrorEvent(WCError(error.throwable)))
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                Timber.d("$TAG onSessionDelete $sessionDelete")
                this@WalletConnectV2.onSessionDelete(sessionDelete)
            }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
                Timber.d("$TAG onSessionProposal $sessionProposal")
                sessionProposal.requiredNamespaces.values.firstOrNull()?.chains?.firstOrNull()?.getChain()?.let { c -> chain = c }
                RxBus.publish(WCEvent.V2(Version.V2, RequestType.SessionProposal))
            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                Timber.d("$TAG onSessionRequest $sessionRequest")
                parseSessionRequest(sessionRequest)
                RxBus.publish(WCEvent.V2(Version.V2, RequestType.SessionRequest))
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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun approveSession(priv: ByteArray) {
        val sessionProposal = Web3Wallet.getSessionProposals().lastOrNull() ?: return

        val pub = ECKeyPair.create(priv).publicKey
        val address = Keys.toChecksumAddress(Keys.getAddress(pub))
        val chains = supportChainList.map { c -> c.chainId }
        val supportAccounts = supportChainList.map { c -> c to address }

        val selectedAccounts: Map<Chain, String> = chains.mapNotNull { namespaceChainId ->
            supportAccounts.firstOrNull { (chain, _) -> chain.chainId == namespaceChainId }
        }.toMap()
        val sessionNamespacesIndexedByNamespace: Map<String, Wallet.Model.Namespace.Session> =
            selectedAccounts.filter { (chain: Chain, _) ->
                sessionProposal.requiredNamespaces
                    .filter { (_, namespace) -> namespace.chains != null }
                    .flatMap { (_, namespace) -> namespace.chains!! }
                    .contains(chain.chainId)
            }.toList().plus(
                selectedAccounts.filter { (chain: Chain, _) ->
                    sessionProposal.optionalNamespaces
                        .filter { (_, namespace) -> namespace.chains != null }
                        .flatMap { (_, namespace) -> namespace.chains!! }
                        .contains(chain.chainId)
                }.toList(),
            )
                .groupBy { (chain: Chain, _: String) -> chain.chainNamespace }
                .asIterable()
                .associate { (key: String, chainData: List<Pair<Chain, String>>) ->
                    val accounts = chainData.map { (chain: Chain, accountAddress: String) ->
                        "${chain.chainNamespace}:${chain.chainReference}:$accountAddress"
                    }
                    val values = sessionProposal.requiredNamespaces.values + sessionProposal.optionalNamespaces.values
                    val methods = values
                        .filter { namespace -> namespace.chains != null }
                        .flatMap { it.methods }
                    val events = values
                        .filter { namespace -> namespace.chains != null }
                        .flatMap { it.events }
                    val chainList: List<String> = values
                        .filter { namespace -> namespace.chains != null }
                        .flatMap { namespace -> namespace.chains!! }

                    key to Wallet.Model.Namespace.Session(
                        accounts = accounts,
                        methods = methods,
                        events = events,
                        chains = chainList.ifEmpty { null },
                    )
                }
        Timber.d("$TAG $sessionNamespacesIndexedByNamespace")

        val sessionNamespacesIndexedByChain: Map<String, Wallet.Model.Namespace.Session> =
            selectedAccounts.filter { (chain: Chain, _) ->
                sessionProposal.requiredNamespaces
                    .filter { (namespaceKey, namespace) -> namespace.chains == null && namespaceKey == chain.chainId }
                    .isNotEmpty()
            }.toList().plus(
                selectedAccounts.filter { (chain: Chain, _) ->
                    sessionProposal.optionalNamespaces
                        .filter { (namespaceKey, namespace) -> namespace.chains == null && namespaceKey == chain.chainId }
                        .isNotEmpty()
                }.toList(),
            ).groupBy { (chain: Chain, _: String) -> chain.chainId }
                .asIterable()
                .associate { (key: String, chainData: List<Pair<Chain, String>>) ->
                    val accounts = chainData.map { (chain: Chain, accountAddress: String) ->
                        "${chain.chainNamespace}:${chain.chainReference}:$accountAddress"
                    }
                    val values = sessionProposal.requiredNamespaces.values + sessionProposal.optionalNamespaces.values
                    val methods = values
                        .filter { namespace -> namespace.chains == null }
                        .flatMap { it.methods }
                    val events = values
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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    private fun parseSessionRequest(request: Wallet.Model.SessionRequest) {
        request.chainId?.getChain()?.let { chain = it }
        when (request.request.method) {
            Method.ETHSign.name -> {
                val array = JsonParser.parseString(request.request.params).asJsonArray
                val address = array[0].toString().trim('"')
                val data = array[1].toString().trim('"')
                Timber.d("$TAG eth sign: $data")
                currentSignData = WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(address, data), WCEthereumSignMessage.WCSignType.MESSAGE), request)
            }
            Method.ETHPersonalSign.name -> {
                val array = JsonParser.parseString(request.request.params).asJsonArray
                val data = array[0].toString().trim('"')
                val address = array[1].toString().trim('"')
                Timber.d("$TAG personal sign: $data")
                currentSignData = WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(data, address), WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE), request)
            }
            Method.ETHSignTypedData.name, Method.ETHSignTypedDataV4.name -> {
                val array = JsonParser.parseString(request.request.params).asJsonArray
                val address = array[0].toString().trim('"')
                val data = array[1].toString().trim('"')
                Timber.d("$TAG sign typed data: $data")
                currentSignData = WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(address, data), WCEthereumSignMessage.WCSignType.TYPED_MESSAGE), request)
            }
            Method.ETHSignTransaction.name -> {
                val transaction = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull() ?: return
                currentSignData = WCSignData.V2SignData(request.request.id, transaction, request)
            }
            Method.ETHSendTransaction.name -> {
                val transaction = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull() ?: return
                currentSignData = WCSignData.V2SignData(request.request.id, transaction, request)
            }
        }
    }

    fun approveRequest(priv: ByteArray) {
        val signData = this.currentSignData ?: return
        if (signData !is WCSignData.V2SignData) {
            Timber.d("TAG signData is not V2SignData")
            return
        }
        val signMessage = signData.signMessage ?: return

        if (signMessage is WCEthereumSignMessage) {
            ethSignMessage(priv, signData.requestId, signData.sessionRequest.topic, signMessage)
        } else if (signMessage is WCEthereumTransaction) {
            when (signData.sessionRequest.request.method) {
                Method.ETHSignTransaction.name -> {
                    ethSignTransaction(priv, signData.requestId, signData.sessionRequest.topic, signMessage, true)
                }
                Method.ETHSendTransaction.name -> {
                    signedTransactionData = ethSignTransaction(priv, signData.requestId, signData.sessionRequest.topic, signMessage, false)
                }
            }
        }
    }

    fun rejectRequest(message: String? = null) {
        val signData = this.currentSignData ?: return
        if (signData !is WCSignData.V2SignData) {
            Timber.d("TAG signData is not V2SignData")
            return
        }
        val request = signData.sessionRequest

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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }

        Web3Wallet.getActiveSessionByTopic(request.topic)?.redirect?.toUri()
            ?.let { deepLinkUri -> sendResponseDeepLink(deepLinkUri) }
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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
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
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }

        // TODO remove?
        Web3Wallet.getActiveSessionByTopic(topic)?.redirect?.toUri()
            ?.let { deepLinkUri -> sendResponseDeepLink(deepLinkUri) }
    }

    fun sendTransaction(id: Long) {
        val signedTransactionData = this.signedTransactionData ?: return
        val topic = (this.currentSignData as? WCSignData.V2SignData<*>)?.sessionRequest?.topic ?: return

        val raw = web3j.ethSendRawTransaction(signedTransactionData).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
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

    fun ethSignMessage(priv: ByteArray, id: Long, topic: String, message: WCEthereumSignMessage) {
        approveRequestInternal(signMessage(priv, message), topic, id)
    }

    private fun ethSignTransaction(priv: ByteArray, id: Long, topic: String, transaction: WCEthereumTransaction, approve: Boolean): String {
        val value = transaction.value ?: "0x0"
        val maxFeePerGas = transaction.maxFeePerGas?.let { Numeric.toBigInt(it) }
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas?.let { Numeric.toBigInt(it) }
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
        val signData = currentSignData as? WCSignData.V2SignData ?: return ""
        val tipGas = signData.tipGas ?: return ""
        val gasLimit = BigInteger(tipGas.gasLimit)
        Timber.d("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit")
        val rawTransaction = if (maxFeePerGas == null && maxPriorityFeePerGas == null) {
            val gasPrice = Convert.toWei(signData.gasPriceType.getGasPrice(tipGas), Convert.Unit.ETHER).toBigInteger()
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
            MixinApplication.appContext.startActivity(
                Intent(Intent.ACTION_VIEW, sessionRequestDeeplinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (exception: ActivityNotFoundException) {
            // There is no app to handle deep link
            Timber.d("$TAG sendResponseDeepLink meet ActivityNotFoundException")
        }
    }

    var onAuthRequest: (authRequest: Wallet.Model.AuthRequest) -> Unit = { _ -> }
    var onConnectionStateChange: (state: Wallet.Model.ConnectionState) -> Unit = { _ -> }
    var onSessionDelete: (sessionDelete: Wallet.Model.SessionDelete) -> Unit = { _ -> }
    var onSessionSettleResponse: (settleSessionResponse: Wallet.Model.SettledSessionResponse) -> Unit = { _ -> }
    var onSessionUpdateResponse: (sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) -> Unit = { _ -> }
    var onPairingDelete: (deletedPairing: Core.Model.DeletedPairing) -> Unit = { _ -> }
}

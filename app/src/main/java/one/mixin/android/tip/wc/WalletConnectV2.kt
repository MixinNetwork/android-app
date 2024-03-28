package one.mixin.android.tip.wc

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.internal.common.exception.GenericException
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.Method
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectException
import one.mixin.android.tip.wc.internal.ethTransactionSerializer
import one.mixin.android.tip.wc.internal.getSupportedNamespaces
import one.mixin.android.tip.wc.internal.supportChainList
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.protocol.core.methods.response.EthGasPrice
import org.web3j.protocol.core.methods.response.EthMaxPriorityFeePerGas
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object WalletConnectV2 : WalletConnect() {
    const val TAG = "WalletConnectV2"

    private val gson =
        GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(ethTransactionSerializer)
            .create()

    init {
        val projectId = BuildConfig.WC_PROJECT_ID
        val serverUrl = "wss://relay.walletconnect.com?projectId=$projectId"
        val appMetaData =
            Core.Model.AppMetaData(
                name = "Mixin Messenger",
                url = "https://messenger.mixin.one",
                description = "An open source cryptocurrency wallet with Signal messaging.",
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
                val err = error.throwable

                // ignore network exceptions
                if (err is GenericException) return@initialize

                RxBus.publish(WCErrorEvent(WCError(error.throwable)))
            },
        )
        val initParams = Wallet.Params.Init(core = CoreClient)
        Web3Wallet.initialize(initParams) { error ->
            Timber.d("$TAG Web3Wallet init error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }

        val coreDelegate =
            object : CoreClient.CoreDelegate {
                override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
                    Timber.d("$TAG onPairingDelete $deletedPairing")
                }

                override fun onPairingExpired(expiredPairing: Core.Model.ExpiredPairing) {
                    Timber.d("$TAG onPairingExpired $expiredPairing")
                }

                override fun onPairingState(pairingState: Core.Model.PairingState) {
                    Timber.d("$TAG onPairingState $pairingState")
                }
            }

        val walletDelegate =
            object : Web3Wallet.WalletDelegate {
                override fun onAuthRequest(
                    authRequest: Wallet.Model.AuthRequest,
                    verifyContext: Wallet.Model.VerifyContext,
                ) {
                    Timber.d("$TAG onAuthRequest $authRequest")
                }

                override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                    Timber.d("$TAG onConnectionStateChange $state")
                }

                override fun onError(error: Wallet.Model.Error) {
                    Timber.d("$TAG onError $error")
                    RxBus.publish(WCErrorEvent(WCError(error.throwable)))
                }

                override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
                    Timber.d("$TAG onProposalExpired")
                }

                override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
                    Timber.d("$TAG onRequestExpired")
                }

                override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                    Timber.d("$TAG onSessionDelete $sessionDelete")
                }

                override fun onSessionExtend(session: Wallet.Model.Session) {
                    Timber.d("$TAG onSessionExtend $session")
                }

                override fun onSessionProposal(
                    sessionProposal: Wallet.Model.SessionProposal,
                    verifyContext: Wallet.Model.VerifyContext,
                ) {
                    Timber.d("$TAG onSessionProposal $sessionProposal")
                    val chains = supportChainList.map { c -> c.chainId }
                    val namespaces =
                        (sessionProposal.requiredNamespaces.values + sessionProposal.optionalNamespaces.values)
                            .filter { proposal -> proposal.chains != null }
                    val hasSupportChain =
                        namespaces.any { proposal ->
                            proposal.chains!!.any { chain ->
                                chains.contains(chain)
                            }
                        }
                    if (hasSupportChain) {
                        RxBus.publish(WCEvent.V2(Version.V2, RequestType.SessionProposal, sessionProposal.pairingTopic))
                    } else {
                        val notSupportChainIds =
                            namespaces.flatMap { proposal ->
                                proposal.chains!!.filter { chain ->
                                    !chains.contains(chain)
                                }
                            }.toSet().joinToString()
                        RxBus.publish(
                            WCErrorEvent(
                                WCError(
                                    IllegalArgumentException(
                                        MixinApplication.appContext.getString(R.string.not_support_network, notSupportChainIds),
                                    ),
                                ),
                            ),
                        )
                    }
                }

                override fun onSessionRequest(
                    sessionRequest: Wallet.Model.SessionRequest,
                    verifyContext: Wallet.Model.VerifyContext,
                ) {
                    Timber.d("$TAG onSessionRequest $sessionRequest")
                    RxBus.publish(WCEvent.V2(Version.V2, RequestType.SessionRequest, sessionRequest.topic))
                }

                override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                    Timber.d("$TAG onSessionSettleResponse $settleSessionResponse")
                }

                override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                    Timber.d("$TAG onSessionUpdateResponse $sessionUpdateResponse")
                }
            }

        CoreClient.setDelegate(coreDelegate)
        Web3Wallet.setWalletDelegate(walletDelegate)
    }

    fun pair(uri: String) {
        RxBus.publish(WCEvent.V2(Version.V2, RequestType.Connect, ""))

        val pairParams = Wallet.Params.Pair(uri)
        Web3Wallet.pair(pairParams, {
            Timber.d("$TAG pair success")
        }) { error ->
            Timber.d("$TAG pair $uri, error: $error")
            RxBus.publish(WCErrorEvent(WCError(WalletConnectException(0, error.throwable.toString() + "\nurl: $uri"))))
        }
    }

    fun ethEstimateGas(
        chain: Chain,
        transaction: Transaction,
    ): EthEstimateGas? {
        return getWeb3j(chain).ethEstimateGas(transaction).send()
    }

    fun ethGasPrice(chain: Chain): EthGasPrice? {
        return getWeb3j(chain).ethGasPrice().send()
    }

    fun ethMaxPriorityFeePerGas(chain: Chain): EthMaxPriorityFeePerGas? {
        return getWeb3j(chain).ethMaxPriorityFeePerGas().send()
    }

    fun approveSession(
        priv: ByteArray,
        topic: String,
        chainId: String,
    ) {
        val sessionProposal = getSessionProposal(topic)
        if (sessionProposal == null) {
            Timber.e("$TAG approveSession sessionProposal is null")
            return
        }
        val address = privateKeyToAddress(priv, chainId)
        val sessionNamespaces = Web3Wallet.generateApprovedNamespaces(sessionProposal, getSupportedNamespaces(chainId, address))
        Timber.d("$TAG approveSession $sessionNamespaces")
        val approveParams: Wallet.Params.SessionApprove =
            Wallet.Params.SessionApprove(
                sessionProposal.proposerPublicKey,
                sessionNamespaces,
            )

        waitActionCheckError { latch ->
            var errMsg: String? = null
            Web3Wallet.approveSession(approveParams, onSuccess = {
                latch.countDown()
            }, onError = { error ->
                errMsg = "$TAG approveSession error: $error"
                Timber.d(errMsg)
                latch.countDown()
            })
            errMsg
        }
    }

    fun rejectSession(topic: String) {
        val sessionProposal = getSessionProposal(topic)
        if (sessionProposal == null) {
            Timber.e("$TAG rejectSession sessionProposal is null")
            return
        }
        rejectSession(sessionProposal)
    }

    fun rejectSession(sessionProposal: Wallet.Model.SessionProposal) {
        val rejectParams: Wallet.Params.SessionReject =
            Wallet.Params.SessionReject(
                sessionProposal.proposerPublicKey,
                "Reject session",
            )
        Web3Wallet.rejectSession(rejectParams) { error ->
            Timber.d("$TAG rejectSession error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun parseSessionRequest(request: Wallet.Model.SessionRequest): WCSignData.V2SignData<*>? {
        val signData =
            when (request.request.method) {
                Method.ETHSign.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val address = array[0].toString().trim('"')
                    val data = array[1].toString().trim('"')
                    Timber.d("$TAG eth sign: $data")
                    WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(address, data), WCEthereumSignMessage.WCSignType.MESSAGE), request)
                }
                Method.ETHPersonalSign.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val data = array[0].toString().trim('"')
                    val address = array[1].toString().trim('"')
                    Timber.d("$TAG personal sign: $data")
                    WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(data, address), WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE), request)
                }
                Method.ETHSignTypedData.name, Method.ETHSignTypedDataV4.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val address = array[0].toString().trim('"')
                    val data = array[1].toString().trim('"')
                    Timber.d("$TAG sign typed data: $data")
                    WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(address, data), WCEthereumSignMessage.WCSignType.TYPED_MESSAGE), request)
                }
                Method.ETHSignTransaction.name -> {
                    val transaction = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull()
                    if (transaction == null) {
                        Timber.e("$TAG parseSessionRequest ETHSignTransaction transaction is null")
                        return null
                    }
                    WCSignData.V2SignData(request.request.id, transaction, request)
                }
                Method.ETHSendTransaction.name -> {
                    val transaction = gson.fromJson<List<WCEthereumTransaction>>(request.request.params).firstOrNull()
                    if (transaction == null) {
                        Timber.e("$TAG parseSessionRequest ETHSendTransaction transaction is null")
                        return null
                    }
                    WCSignData.V2SignData(request.request.id, transaction, request)
                }
                else -> {
                    Timber.e("$TAG parseSessionRequest not supported method ${request.request.method}")
                    null
                }
            }
        return signData
    }

    fun approveRequest(
        priv: ByteArray,
        chain: Chain,
        topic: String,
        signData: WCSignData.V2SignData<*>,
    ): String? {
        val sessionRequest = getSessionRequest(topic)
        if (sessionRequest == null) {
            Timber.e("$TAG approveRequest sessionRequest is null")
            return null
        }

        val signMessage = signData.signMessage
        if (signMessage is WCEthereumSignMessage) {
            @Suppress("UNCHECKED_CAST")
            ethSignMessage(priv, sessionRequest, signData as WCSignData.V2SignData<WCEthereumSignMessage>)
        } else if (signMessage is WCEthereumTransaction) {
            @Suppress("UNCHECKED_CAST")
            signData as WCSignData.V2SignData<WCEthereumTransaction>
            when (signData.sessionRequest.request.method) {
                Method.ETHSignTransaction.name -> {
                    ethSignTransaction(priv, chain, sessionRequest, signData, true)
                }
                Method.ETHSendTransaction.name -> {
                    return ethSignTransaction(priv, chain, sessionRequest, signData, false)
                }
            }
        }
        return null
    }

    fun rejectRequest(
        message: String? = null,
        topic: String,
    ) {
        val request = getSessionRequest(topic)
        if (request == null) {
            Timber.e("$TAG rejectRequest sessionRequest is null")
            return
        }
        rejectRequest(message, request)
    }

    fun rejectRequest(
        message: String? = null,
        request: Wallet.Model.SessionRequest,
    ) {
        val result =
            Wallet.Params.SessionRequestResponse(
                sessionTopic = request.topic,
                jsonRpcResponse =
                    Wallet.Model.JsonRpcResponse.JsonRpcError(
                        id = request.request.id,
                        code = 500,
                        message = message ?: "Mixin Wallet Error",
                    ),
            )
        Web3Wallet.respondSessionRequest(result) { error ->
            Timber.d("$TAG rejectSessionRequest error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun getListOfActiveSessions(): List<Wallet.Model.Session> {
        return try {
            Web3Wallet.getListOfActiveSessions()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getListOfActiveSessions ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun getSessionProposal(topic: String): Wallet.Model.SessionProposal? {
        Timber.d("$TAG getSessionProposal topic: $topic")
        return try {
            Web3Wallet.getSessionProposals().find { sp ->
                sp.pairingTopic == topic
            }
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getSessionProposal ${e.stackTraceToString()}")
            null
        }
    }

    fun getSessionRequest(topic: String): Wallet.Model.SessionRequest? {
        return try {
            Web3Wallet.getPendingListOfSessionRequests(topic).firstOrNull()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getSessionRequest ${e.stackTraceToString()}")
            null
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

    private fun approveRequestInternal(
        result: String,
        sessionRequest: Wallet.Model.SessionRequest,
    ) {
        Timber.d("$TAG approve request $result")
        val response =
            Wallet.Params.SessionRequestResponse(
                sessionTopic = sessionRequest.topic,
                jsonRpcResponse =
                    Wallet.Model.JsonRpcResponse.JsonRpcResult(
                        sessionRequest.request.id,
                        result,
                    ),
            )

        waitActionCheckError { latch ->
            var errMsg: String? = null
            Web3Wallet.respondSessionRequest(response, {
                latch.countDown()
            }) { error ->
                errMsg = "$TAG approveSessionRequest error: $error"
                Timber.d(errMsg)
                latch.countDown()
            }
            errMsg
        }
    }

    fun sendTransaction(
        chain: Chain,
        sessionRequest: Wallet.Model.SessionRequest,
        signedTransactionData: String,
    ) {
        val tx = getWeb3j(chain).ethSendRawTransaction(signedTransactionData).send()
        if (tx.hasError()) {
            val msg = "error code: ${tx.error.code}, message: ${tx.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            rejectRequest(msg, sessionRequest)
            throw WalletConnectException(tx.error.code, tx.error.message)
        }
        val transactionHash = tx.transactionHash
        Timber.d("$TAG sendTransaction $transactionHash")
        approveRequestInternal(transactionHash, sessionRequest)
    }

    private fun ethSignMessage(
        priv: ByteArray,
        sessionRequest: Wallet.Model.SessionRequest,
        signData: WCSignData.V2SignData<WCEthereumSignMessage>,
    ) {
        approveRequestInternal(signMessage(priv, signData.signMessage), sessionRequest)
    }

    private fun ethSignTransaction(
        priv: ByteArray,
        chain: Chain,
        sessionRequest: Wallet.Model.SessionRequest,
        signData: WCSignData.V2SignData<WCEthereumTransaction>,
        approve: Boolean,
    ): String {
        val transaction = signData.signMessage
        val value = transaction.value ?: "0x0"
        val maxFeePerGas = transaction.maxFeePerGas?.let { Numeric.toBigInt(it) }
        val maxPriorityFeePerGas = transaction.maxPriorityFeePerGas?.let { Numeric.toBigInt(it) }
        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val transactionCount =
            getWeb3j(chain).ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST).send()
        if (transactionCount.hasError()) {
            throwError(transactionCount.error)
        }
        val nonce = transactionCount.transactionCount
        val v = Numeric.toBigInt(value)
        val tipGas = signData.tipGas
        if (tipGas == null) {
            Timber.e("$TAG ethSignTransaction tipGas is null")
            throw IllegalArgumentException("TipGas is null")
        }
        val gasLimit = tipGas.gasLimit
        Timber.e("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit")
        val rawTransaction =
            if (maxFeePerGas == null && maxPriorityFeePerGas == null) {
                val gasPrice = Convert.toWei(tipGas.gasPrice.toBigDecimal(), Convert.Unit.WEI).toBigInteger()
                Timber.e("$TAG gasPrice $gasPrice")
                RawTransaction.createTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    transaction.to,
                    v,
                    transaction.data ?: "",
                )
            } else {
                Timber.e("$TAG maxFeePerGas $maxFeePerGas maxPriorityFeePerGas $maxPriorityFeePerGas")
                RawTransaction.createTransaction(
                    chain.chainReference.toLong(),
                    nonce,
                    gasLimit,
                    transaction.to,
                    v,
                    transaction.data ?: "",
                    maxPriorityFeePerGas,
                    maxFeePerGas,
                )
            }

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        if (approve) {
            approveRequestInternal(hexMessage, sessionRequest)
        }
        return hexMessage
    }

    @Suppress("unused")
    private fun ethSendTransaction(
        web3j: Web3j,
        priv: ByteArray,
        chain: Chain,
        sessionRequest: Wallet.Model.SessionRequest,
        signData: WCSignData.V2SignData<WCEthereumTransaction>,
    ) {
        val hexMessage = ethSignTransaction(priv, chain, sessionRequest, signData, false)
        val result = web3j.ethSendRawTransaction(hexMessage).send()
        if (result.hasError()) {
            val msg = "error code: ${result.error.code}, message: ${result.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            rejectRequest(msg, sessionRequest)
            throw WalletConnectException(result.error.code, result.error.message)
        } else {
            val transactionHash = result.transactionHash
            Timber.d("$TAG sendTransaction $transactionHash")
            approveRequestInternal(transactionHash, sessionRequest)
        }
    }

    private fun waitActionCheckError(action: (CountDownLatch) -> String?) {
        val latch = CountDownLatch(1)
        val errMsg = action.invoke(latch)
        try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw WalletConnectException(0, e.toString())
        }
        errMsg?.let { throw WalletConnectException(0, it) }
    }
}

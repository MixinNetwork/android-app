package one.mixin.android.tip.wc

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.internal.common.exception.GenericException
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.db.web3.vo.Web3RawTransaction
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.Method
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectException
import one.mixin.android.tip.wc.internal.WcSignature
import one.mixin.android.tip.wc.internal.WcSolanaMessage
import one.mixin.android.tip.wc.internal.WcSolanaTransaction
import one.mixin.android.tip.wc.internal.ethTransactionSerializer
import one.mixin.android.tip.wc.internal.getSupportedNamespaces
import one.mixin.android.tip.wc.internal.supportChainList
import one.mixin.android.ui.tip.wc.WalletUnlockBottomSheetDialogFragment
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.encodeToBase58String
import org.sol4k.Keypair
import org.sol4kt.VersionedTransactionCompat
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
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

                // RxBus.publish(WCErrorEvent(WCError(error.throwable)))
            },
        )
        val initParams = Wallet.Params.Init(core = CoreClient)
        WalletKit.initialize(initParams) { error ->
            Timber.d("$TAG Web3Wallet init error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }

        val coreDelegate =
            object : CoreClient.CoreDelegate {
                override fun onPairingState(pairingState: Core.Model.PairingState) {
                    Timber.d("$TAG onPairingState $pairingState")
                }
            }

        val walletDelegate =
            object : WalletKit.WalletDelegate {
                override val onSessionAuthenticate: ((Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit)?
                    get() = super.onSessionAuthenticate

                override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                    Timber.d("$TAG onConnectionStateChange $state")
                }

                override fun onError(error: Wallet.Model.Error) {
                    Timber.d("$TAG onError $error")
                    // RxBus.publish(WCErrorEvent(WCError(error.throwable)))
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

                    val namespace = sessionProposal.requiredNamespaces.values.firstOrNull() ?: sessionProposal.optionalNamespaces.values.firstOrNull()
                    if (namespace == null) {
                        RxBus.publish(
                            WCErrorEvent(
                                WCError(
                                    IllegalArgumentException(
                                        "Empty namespace",
                                    ),
                                ),
                            ),
                        )
                        return
                    }
                    val requireChain =
                        supportChainList.firstOrNull {
                            (namespace).chains?.contains(it.chainId) == true
                        }
                    val chainType =
                        when {
                            requireChain is Chain.Solana -> WalletUnlockBottomSheetDialogFragment.TYPE_SOLANA
                            requireChain is Chain.BinanceSmartChain -> WalletUnlockBottomSheetDialogFragment.TYPE_BSC
                            requireChain is Chain.Polygon -> WalletUnlockBottomSheetDialogFragment.TYPE_POLYGON
                            else -> WalletUnlockBottomSheetDialogFragment.TYPE_ETH
                        }

                    if (hasSupportChain) {
                        RxBus.publish(WCEvent.V2(Version.V2, RequestType.SessionProposal, sessionProposal.pairingTopic, chainType))
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
        WalletKit.setWalletDelegate(walletDelegate)
    }

    fun pair(uri: String) {
        RxBus.publish(WCEvent.V2(Version.V2, RequestType.Connect, ""))

        val pairParams = Wallet.Params.Pair(uri)
        WalletKit.pair(pairParams, {
            Timber.d("$TAG pair success")
        }) { error ->
            Timber.d("$TAG pair $uri, error: $error")
            RxBus.publish(WCErrorEvent(WCError(WalletConnectException(0, error.throwable.toString() + "\nurl: $uri"))))
        }
    }

    private fun <T> flattenCollections(collection: List<List<T>?>): List<T> {
        val result = mutableListOf<T>()
        for (innerCollection in collection) {
            if (innerCollection == null) continue
            result.addAll(innerCollection)
        }
        return result
    }

    fun approveSession(
        priv: ByteArray,
        topic: String,
    ) {
        val sessionProposal = getSessionProposal(topic)
        if (sessionProposal == null) {
            Timber.e("$TAG approveSession sessionProposal is null")
            return
        }
        val requiredNamespaces: Collection<String> = flattenCollections(sessionProposal.requiredNamespaces.values.map { it.chains })
        val chain =
            if (requiredNamespaces.isEmpty()) {
                supportChainList.firstOrNull()
            } else {
                supportChainList.find {
                    it.chainId in requiredNamespaces
                }
            }
        if (chain == null) {
            Timber.e("$TAG approveSession sessionProposal chain is null")
            return
        }
        val address =
            if (chain == Chain.Solana) {
                Keypair.fromSecretKey(priv).publicKey.toBase58()
            } else {
                val pub = ECKeyPair.create(priv).publicKey
                Keys.toChecksumAddress(Keys.getAddress(pub))
            }

        val supportedNamespaces = getSupportedNamespaces(chain, address)
        Timber.e("$TAG supportedNamespaces $supportedNamespaces")
        val sessionNamespaces = WalletKit.generateApprovedNamespaces(sessionProposal, supportedNamespaces)
        Timber.d("$TAG approveSession $sessionNamespaces")
        val approveParams: Wallet.Params.SessionApprove =
            Wallet.Params.SessionApprove(
                sessionProposal.proposerPublicKey,
                sessionNamespaces,
            )

        waitActionCheckError { latch ->
            var errMsg: String? = null
            WalletKit.approveSession(approveParams, onSuccess = {
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
        WalletKit.rejectSession(rejectParams) { error ->
            Timber.d("$TAG rejectSession error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun parseSessionRequest(
        localAddress: String,
        request: Wallet.Model.SessionRequest,
    ): WCSignData.V2SignData<*>? {
        val signData =
            when (request.request.method) {
                Method.ETHSign.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val address = array[0].toString().trim('"')
                    val data = array[1].toString().trim('"')
                    Timber.d("$TAG eth sign: $data")
                    if (localAddress.isNotBlank() && !address.equals(localAddress, true)) {
                        throw IllegalArgumentException("Address unequal")
                    }
                    WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(address, data), WCEthereumSignMessage.WCSignType.MESSAGE), request)
                }

                Method.ETHPersonalSign.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val data = array[0].toString().trim('"')
                    val address = array[1].toString().trim('"')
                    Timber.d("$TAG personal sign: $data")
                    if (localAddress.isNotBlank() && !address.equals(localAddress, true)) {
                        throw IllegalArgumentException("Address unequal")
                    }
                    WCSignData.V2SignData(request.request.id, WCEthereumSignMessage(listOf(data, address), WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE), request)
                }

                Method.ETHSignTypedData.name, Method.ETHSignTypedDataV4.name -> {
                    val array = JsonParser.parseString(request.request.params).asJsonArray
                    val address = array[0].toString().trim('"')
                    val data = array[1].toString().trim('"')
                    Timber.d("$TAG sign typed data: $data")
                    if (localAddress.isNotBlank() && !address.equals(localAddress, true)) {
                        throw IllegalArgumentException("Address unequal")
                    }
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
                Method.SolanaSignTransaction.name -> {
                    val transaction = gson.fromJson<WcSolanaTransaction>(request.request.params)
                    val tx = VersionedTransactionCompat.from(transaction.transaction)
                    WCSignData.V2SignData(request.request.id, tx, request, solanaFee = tx.calcFee())
                }
                Method.SolanaSignMessage.name -> {
                    val message = gson.fromJson<WcSolanaMessage>(request.request.params)
                    WCSignData.V2SignData(request.request.id, message, request)
                }
                else -> {
                    Timber.e("$TAG ${request.request.method} parseSessionRequest not supported method ${request.request.method}")
                    null
                }
            }
        return signData
    }

    suspend fun approveRequest(
        priv: ByteArray,
        chain: Chain,
        topic: String,
        signData: WCSignData.V2SignData<*>,
        getBlockhash: suspend () -> String,
        getNonce: suspend (String) -> BigInteger
    ): Any? {
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
                    return ethSignTransaction(priv, chain, sessionRequest, signData, true, getNonce)
                }
                Method.ETHSendTransaction.name -> {
                    return ethSignTransaction(priv, chain, sessionRequest, signData, false, getNonce)
                }
            }
        } else if (signMessage is VersionedTransactionCompat) {
            val holder = Keypair.fromSecretKey(priv)
            // use latest blockhash should not break other signatures
            if (signMessage.signatures.size <= 1) {
                signMessage.message.recentBlockhash = getBlockhash()
            }
            signMessage.sign(holder)
            return signMessage
        } else if (signMessage is WcSolanaMessage) {
            val holder = Keypair.fromSecretKey(priv)
            val message = signMessage.message.decodeBase58()
            val sig = holder.sign(message).encodeToBase58String()
            val wcSig = WcSignature(signMessage.pubkey, sig)
            approveRequestInternal(gson.toJson(wcSig), sessionRequest)
            return null
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
        WalletKit.respondSessionRequest(result) { error ->
            Timber.d("$TAG rejectSessionRequest error: $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun getListOfActiveSessions(): List<Wallet.Model.Session> {
        return try {
            WalletKit.getListOfActiveSessions()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getListOfActiveSessions ${e.stackTraceToString()}")
            emptyList()
        }
    }

    fun getSessionProposal(topic: String): Wallet.Model.SessionProposal? {
        Timber.d("$TAG getSessionProposal topic: $topic")
        return try {
            WalletKit.getSessionProposals().find { sp ->
                sp.pairingTopic == topic
            }
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getSessionProposal ${e.stackTraceToString()}")
            null
        }
    }

    fun getSessionRequest(topic: String): Wallet.Model.SessionRequest? {
        return try {
            WalletKit.getPendingListOfSessionRequests(topic).firstOrNull()
        } catch (e: IllegalStateException) {
            Timber.d("$TAG getSessionRequest ${e.stackTraceToString()}")
            null
        }
    }

    fun disconnect(topic: String) {
        WalletKit.disconnectSession(
            Wallet.Params.SessionDisconnect(topic),
            onSuccess = {
                Timber.d("$TAG disconnect success")
            },
        ) { error ->
            Timber.d("$TAG disconnect error $error")
            RxBus.publish(WCErrorEvent(WCError(error.throwable)))
        }
    }

    fun approveRequestInternal(
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
            WalletKit.respondSessionRequest(response, {
                latch.countDown()
            }) { error ->
                errMsg = "$TAG approveSessionRequest error: $error"
                Timber.d(errMsg)
                latch.countDown()
            }
            errMsg
        }
    }


    private fun ethSignMessage(
        priv: ByteArray,
        sessionRequest: Wallet.Model.SessionRequest,
        signData: WCSignData.V2SignData<WCEthereumSignMessage>,
    ) {
        approveRequestInternal(signMessage(priv, signData.signMessage), sessionRequest)
    }

    private suspend fun ethSignTransaction(
        priv: ByteArray,
        chain: Chain,
        sessionRequest: Wallet.Model.SessionRequest,
        signData: WCSignData.V2SignData<WCEthereumTransaction>,
        approve: Boolean,
        getNonce: suspend (String) -> BigInteger,
    ): String {
        val transaction = signData.signMessage
        val value = transaction.value ?: "0x0"

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val nonce = getNonce(credential.address)
        val v = Numeric.decodeQuantity(value)
        val tipGas = signData.tipGas
        if (tipGas == null) {
            Timber.e("$TAG ethSignTransaction tipGas is null")
            throw IllegalArgumentException("TipGas is null")
        }

        val maxPriorityFeePerGas = tipGas.maxPriorityFeePerGas
        val maxFeePerGas = tipGas.selectMaxFeePerGas(transaction.maxFeePerGas?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO)
        val gasLimit = tipGas.gasLimit
        Timber.e("$TAG dapp gas: ${transaction.gas?.let { Numeric.decodeQuantity(it) }} gasLimit: ${transaction.gasLimit?.let { Numeric.decodeQuantity(it) }} maxFeePerGas: ${transaction.maxFeePerGas?.let { Numeric.decodeQuantity(it) }} maxPriorityFeePerGas: ${transaction.maxPriorityFeePerGas?.let { Numeric.decodeQuantity(it) }} ")
        Timber.e("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit maxFeePerGas: $maxFeePerGas maxPriorityFeePerGas: $maxPriorityFeePerGas")
        val rawTransaction =
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
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        if (approve) {
            approveRequestInternal(hexMessage, sessionRequest)
        }
        return hexMessage
    }

    fun approveSolanaTransaction(
        signature: String,
        sessionRequest: Wallet.Model.SessionRequest,
    ) {
        val wcSig = WcSignature("", signature)
        approveRequestInternal(gson.toJson(wcSig), sessionRequest)
    }

    private fun waitActionCheckError(action: (CountDownLatch) -> String?) {
        val latch = CountDownLatch(1)
        val errMsg = action.invoke(latch)
        try {
            latch.await(30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw WalletConnectException(0, e.toString())
        }
        errMsg?.let { throw WalletConnectException(0, it) }
    }
}

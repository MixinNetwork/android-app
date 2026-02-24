package one.mixin.android.ui.tip

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.BTC_ADDRESS
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexString
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toHex
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipBody
import one.mixin.android.tip.TipConstants.tipNodeApi2Path
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.TipNotAllWatcherSuccessException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.web3j.utils.Numeric
import retrofit2.HttpException
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@ActivityRetainedScoped
class TipFlowInteractor @Inject internal constructor(
    private val tip: Tip,
    private val tipNodeService: TipNodeService,
    private val tipConfig: TipConfig,
    private val accountService: AccountService,
    private val utxoService: UtxoService,
    private val pinCipher: PinCipher,
    private val web3Repository: Web3Repository,
) {
    suspend fun tryConnect(
        context: Context,
        tipBundle: TipBundle,
        shouldWatch: Boolean,
        onStepChanged: (TipStep) -> Unit,
    ) {
        onStepChanged(TryConnecting)
        val (available, info) = if (shouldWatch) {
            val tipCounter = Session.getTipCounter()
            var isAvailable = false
            var message = ""
            tip.checkCounter(
                tipCounter,
                onNodeCounterNotEqualServer = { nodeMaxCounter, nodeFailedSigners ->
                    tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                },
                onNodeCounterInconsistency = { nodeMaxCounter, nodeFailedSigners ->
                    tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                },
            ).onSuccess {
                isAvailable = true
            }.onFailure {
                isAvailable = false
                if (it is TipNotAllWatcherSuccessException) {
                    message = it.info
                }
            }
            Pair(isAvailable, message)
        } else {
            checkTipNodeConnect()
        }
        if (available) {
            onStepChanged(ReadyStart)
        } else {
            onStepChanged(RetryConnect(shouldWatch, info))
        }
    }

    suspend fun process(
        context: Context,
        tipBundle: TipBundle,
        shouldOpenMainActivity: Boolean,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        val pin: String = requireNotNull(tipBundle.pin) { "required pin can not be null" }
        val oldPin: String? = tipBundle.oldPin
        val tipCounter = Session.getTipCounter()
        val deviceId = tipBundle.deviceId
        val nodeCounter = tipBundle.tipEvent?.nodeCounter ?: tipCounter
        val failedSigners = tipBundle.tipEvent?.failedSigners
        var nodeFailedInfo = ""
        val observer: Tip.Observer = object : Tip.Observer {
            override fun onSyncing(step: Int, total: Int) {
                onStepChanged(Processing.SyncingNode(step, total))
            }
            override fun onSyncingComplete() {
                onStepChanged(Processing.Updating)
            }
            override fun onNodeFailed(info: String) {
                nodeFailedInfo = info
            }
        }
        onStepChanged(Processing.Creating)
        tip.addObserver(observer)
        val tipPriv: ByteArray? = try {
            val result: Result<ByteArray?> =
                if (tipCounter < 1) {
                    tip.createTipPriv(context, pin, deviceId, failedSigners, oldPin)
                } else {
                    val requiredOldPin: String = requireNotNull(oldPin) { "required oldPin can not be null" }
                    tip.updateTipPriv(context, deviceId, pin, requiredOldPin, nodeCounter == tipCounter, failedSigners)
                }
            result.getOrThrow()
        } catch (e: Throwable) {
            tip.removeObserver(observer)
            return handleProcessFailure(context, e, tipBundle, pin, tipCounter, nodeCounter, nodeFailedInfo, onStepChanged, onShowMessage)
        } finally {
            tip.removeObserver(observer)
        }
        return handleProcessSuccess(context, tipBundle, pin, tipPriv, shouldOpenMainActivity, onStepChanged, onShowMessage)
    }

    private suspend fun handleProcessFailure(
        context: Context,
        e: Throwable,
        tipBundle: TipBundle,
        pin: String,
        tipCounter: Int,
        nodeCounterBeforeRequest: Int,
        nodeFailedInfo: String,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        val extraInfo = "account counter: $tipCounter, nodeCounterBeforeRequest: $nodeCounterBeforeRequest, type: ${tipBundle.tipType}, step: ${tipBundle.tipStep}, event: ${tipBundle.tipEvent}"
        val errMsg = e.getTipExceptionMsg(context, nodeFailedInfo, extraInfo)
        onShowMessage(errMsg)
        if (e is DifferentIdentityException) {
            tipBundle.oldPin = null
            tipBundle.pin = null
            onStepChanged(RetryConnect(true, ""))
            return false
        }
        tip.checkCounter(
            tipCounter,
            onNodeCounterNotEqualServer = { nodeMaxCounter, nodeFailedSigners ->
                tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
            },
            onNodeCounterInconsistency = { nodeMaxCounter, nodeFailedSigners ->
                tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
            },
        ).onFailure {
            // Generally, check-counter should NOT meet exceptions, if this happens,
            // we should go to the RetryConnect step to check network and other steps.
            tipBundle.pin = null
            tipBundle.oldPin = null
            val errorInfo = if (it is TipNotAllWatcherSuccessException) it.info else ""
            onStepChanged(RetryConnect(true, errorInfo))
            return false
        }

        // all signer failed perhaps means PIN incorrect, clear PIN and let user re-input.
        if (e is NotAllSignerSuccessException && e.allFailure()) {
            tipBundle.pin = null
        }

        val newNodeCounter = tipBundle.tipEvent?.nodeCounter ?: nodeCounterBeforeRequest
        if (newNodeCounter > nodeCounterBeforeRequest && newNodeCounter > tipCounter) {
            // If new node counter greater than session counter and old node counter,
            // we should refresh session counter to prevent failure in cases where
            // pin/update completes but local session account update fails.
            refreshAccount(context)
            val newSessionCounter = Session.getTipCounter()
            // If new session counter equals new node counter,
            // we consider this case as PIN update success.
            if (newNodeCounter == newSessionCounter) {
                return handleProcessSuccess(context, tipBundle, pin, null, true, onStepChanged, onShowMessage)
            }
        }
        onStepChanged(RetryProcess(errMsg))
        return false
    }

    private suspend fun handleProcessSuccess(
        context: Context,
        tipBundle: TipBundle,
        pin: String,
        tipPriv: ByteArray?,
        shouldOpenMainActivity: Boolean,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        if (!Session.hasSafe()) {
            val registerResult: Boolean = try {
                registerPublicKey(context, tipBundle, pin, tipPriv, onStepChanged, onShowMessage)
            } catch (e: Exception) {
                val errorInfo = e.getTipExceptionMsg(context)
                onStepChanged(RetryRegister(null, errorInfo))
                false
            }
            if (!registerResult) {
                // register public key failed, already go to RetryRegister
                return false
            }
        }
        val cur = System.currentTimeMillis()
        context.defaultSharedPreferences.putBoolean(PREF_LOGIN_OR_SIGN_UP, true)
        context.defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
        putPrefPinInterval(context, INTERVAL_10_MINS)
        val openBiometrics = context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (openBiometrics) {
            try {
                BiometricUtil.savePin(context, pin)
            } catch (ignored: UserNotAuthenticatedException) {
                onShowMessage(ignored.toString())
            }
        }
        if (shouldOpenMainActivity) {
            MainActivity.show(context)
        }
        return true
    }

    private suspend fun registerPublicKey(
        context: Context,
        tipBundle: TipBundle,
        pin: String,
        tipPriv: ByteArray?,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        onStepChanged(Processing.Registering)
        val meResp = accountService.getMeSuspend()
        if (meResp.isSuccess) {
            val account = requireNotNull(meResp.data) { "required account can not be null" }
            Session.storeAccount(account)
            if (account.hasSafe) {
                return true
            }
        } else {
            tipBundle.oldPin = null
            val error = requireNotNull(meResp.error) { "error can not be null" }
            val errorInfo = context.getMixinErrorStringByCode(error.code, error.description)
            onStepChanged(RetryRegister(tipPriv, errorInfo))
            return false
        }
        val seed = try {
            tipPriv ?: tip.getOrRecoverTipPriv(context, pin).getOrThrow()
        } catch (e: Exception) {
            tipBundle.oldPin = null
            val errorInfo = e.getTipExceptionMsg(context)
            onStepChanged(RetryRegister(null, errorInfo))
            return false
        }
        val spendSeed = tip.getSpendPriv(context, seed)
        val saltBase64 = tip.getEncryptSalt(context, pin, seed, Session.isAnonymous())
        val spendKeyPair = newKeyPairFromSeed(spendSeed)
        val selfAccountId = requireNotNull(Session.getAccountId()) { "self userId can not be null at this step" }
        val edKey = tip.getMnemonicEdKey(context, pin, seed)
        val pkHex: String = spendKeyPair.publicKey.toHex()
        val registerRequest = RegisterRequest(
            publicKey = pkHex,
            signature = Session.getRegisterSignature(selfAccountId, spendSeed),
            pin = getEncryptedTipBody(selfAccountId, pkHex, pin),
            salt = saltBase64,
            masterPublicHex = edKey.publicKey.hexString(),
            masterSignatureHex = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), selfAccountId.toByteArray()).hexString(),
        )
        val registerResp = utxoService.registerPublicKey(registerRequest)
        if (registerResp.isSuccess) {
            val solAddress: String = getTipAddress(context, pin, SOLANA_CHAIN_ID)
            PropertyHelper.updateKeyValue(SOLANA_ADDRESS, solAddress)
            val evmAddress: String = getTipAddress(context, pin, ETHEREUM_CHAIN_ID)
            PropertyHelper.updateKeyValue(EVM_ADDRESS, evmAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, solAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, evmAddress)
            Session.storeAccount(requireNotNull(registerResp.data) { "required account can not be null" })
            createWallet(context, spendSeed)
            if (Session.hasPhone()) {
                removeValueFromEncryptedPreferences(context, Constants.Tip.MNEMONIC)
            }
            return true
        }
        if (registerResp.errorCode == ErrorHandler.INVALID_PIN_FORMAT) {
            tipBundle.oldPin = null
            onStepChanged(LegacyPIN(context.getString(R.string.error_legacy_pin)))
            return false
        }
        tipBundle.oldPin = null
        val error = requireNotNull(registerResp.error) { "error can not be null" }
        val errorInfo = context.getMixinErrorStringByCode(error.code, error.description)
        onStepChanged(RetryRegister(tipPriv, errorInfo))
        return false
    }

    private suspend fun createWallet(context: Context, spendKey: ByteArray) {
        val hasClassicWallet: Boolean = web3Repository.getClassicWalletId() != null
        if (hasClassicWallet) {
            return
        }
        val walletName: String = context.getString(R.string.Common_Wallet)
        val classicIndex = 0
        val evmAddress: String = privateKeyToAddress(spendKey, ETHEREUM_CHAIN_ID, classicIndex)
        val solAddress: String = privateKeyToAddress(spendKey, SOLANA_CHAIN_ID, classicIndex)
        val btcAddress: String = privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex)
        val addresses: List<Web3AddressRequest> = listOf(
            createSignedWeb3AddressRequest(destination = evmAddress, chainId = ETHEREUM_CHAIN_ID, path = Bip44Path.ethereumPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, ETHEREUM_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
            createSignedWeb3AddressRequest(destination = solAddress, chainId = SOLANA_CHAIN_ID, path = Bip44Path.solanaPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, SOLANA_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
            createSignedWeb3AddressRequest(destination = btcAddress, chainId = Constants.ChainId.BITCOIN_CHAIN_ID, path = Bip44Path.bitcoinSegwitPathString(classicIndex), privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex), category = WalletCategory.CLASSIC.value),
        )
        val walletRequest = WalletRequest(name = walletName, category = WalletCategory.CLASSIC.value, addresses = addresses)
        requestRouteAPI(
            invokeNetwork = { web3Repository.createWallet(walletRequest) },
            successBlock = { response ->
                response.data?.let { wallet ->
                    web3Repository.insertWallet(Web3Wallet(id = wallet.id, name = wallet.name, category = wallet.category, createdAt = wallet.createdAt, updatedAt = wallet.updatedAt))
                    val walletAddresses = wallet.addresses ?: emptyList()
                    if (walletAddresses.isNotEmpty()) {
                        web3Repository.insertAddressList(walletAddresses)
                    }
                }
            },
            requestSession = { ids ->
                web3Repository.fetchSessionsSuspend(ids)
            },
        )
    }

    private fun createSignedWeb3AddressRequest(
        destination: String,
        chainId: String,
        path: String?,
        privateKey: ByteArray,
        category: String,
    ): Web3AddressRequest {
        val selfId = Session.getAccountId()
        if (category == WalletCategory.WATCH_ADDRESS.value) {
            return Web3AddressRequest(destination = destination, chainId = chainId, path = path)
        }
        val now = Instant.now()
        val message = "$destination\n$selfId\n${now.epochSecond}"
        val signature = if (chainId == SOLANA_CHAIN_ID) {
            Numeric.prependHexPrefix(Web3Signer.signSolanaMessage(privateKey, message.toByteArray()))
        } else if (chainId in Constants.Web3EvmChainIds) {
            Web3Signer.signEthMessage(privateKey, message.toByteArray().toHexString(), JsSignMessage.TYPE_PERSONAL_MESSAGE)
        } else if (chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
            val ecKey: ECKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
            Numeric.toHexString(ecKey.signMessage(message, ScriptType.P2WPKH).decodeBase64())
        } else {
            null
        }
        return Web3AddressRequest(destination = destination, chainId = chainId, path = path, signature = signature, timestamp = now.toString())
    }

    private suspend fun getEncryptedTipBody(userId: String, pkHex: String, pin: String): String =
        pinCipher.encryptPin(pin, TipBody.forSequencerRegister(userId, pkHex))

    private suspend fun getTipAddress(context: Context, pin: String, chainId: String): String {
        val result = tip.getOrRecoverTipPriv(context, pin)
        val spendKey = tip.getSpendPrivFromEncryptedSalt(
            tip.getMnemonicFromEncryptedPreferences(context),
            tip.getEncryptedSalt(context),
            pin,
            result.getOrThrow(),
        )
        return privateKeyToAddress(spendKey, chainId)
    }

    private suspend fun refreshAccount(context: Context) {
        handleMixinResponse(
            invokeNetwork = { accountService.getMeSuspend() },
            successBlock = { r ->
                r.data?.let { Session.storeAccount(it) }
            },
        )
    }

    private suspend fun checkTipNodeConnect(): Pair<Boolean, String> {
        val signers = tipConfig.signers
        val nodeFailedInfo = StringBuffer()
        val successSum = AtomicInteger(0)
        coroutineScope {
            signers.map { signer ->
                async(Dispatchers.IO) {
                    kotlin.runCatching {
                        tipNodeService.get(tipNodeApi2Path(signer.api))
                        successSum.incrementAndGet()
                    }.onFailure {
                        if (it is HttpException) {
                            nodeFailedInfo.append("[${signer.index}, ${it.code()}] ")
                        }
                    }
                }
            }.awaitAll()
        }
        return Pair(successSum.get() == signers.size, nodeFailedInfo.toString())
    }
}

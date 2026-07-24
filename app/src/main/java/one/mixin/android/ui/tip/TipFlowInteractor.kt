package one.mixin.android.ui.tip

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.response.TipConfig
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.TipNodeService
import one.mixin.android.api.service.UtxoService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.crypto.hasPendingImportMnemonic
import one.mixin.android.crypto.hasPendingImportMnemonicInMemory
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.markPendingImportMnemonic
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findFragmentActivityOrNull
import one.mixin.android.extension.hexString
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toHex
import one.mixin.android.repository.UserRepository
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipBody
import one.mixin.android.tip.TipConstants.tipNodeApi2Path
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.TipNotAllWatcherSuccessException
import one.mixin.android.tip.getSpendKeyFromPin
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.INITIAL_CLASSIC_WALLET_INDEX
import one.mixin.android.ui.wallet.buildClassicWalletRequest
import one.mixin.android.ui.wallet.ensureInitialClassicWallet
import one.mixin.android.ui.wallet.WalletSecurityActivity
import one.mixin.android.ui.wallet.components.walletDestinationForWallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import retrofit2.HttpException
import timber.log.Timber
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
    private val userRepository: UserRepository,
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
        lifecycleScope: CoroutineScope,
        tipBundle: TipBundle,
        shouldOpenMainActivity: Boolean,
        onStepChanged: (TipStep) -> Unit,
        onShowMessage: (String) -> Unit,
    ): Boolean {
        val pin: String = requireNotNull(tipBundle.pin) { "required pin can not be null" }
        val retryRegister = tipBundle.tipStep as? RetryRegister
        if (retryRegister != null) {
            return handleProcessSuccess(
                context = context,
                tipBundle = tipBundle,
                pin = pin,
                tipPriv = retryRegister.tipPriv,
                shouldOpenMainActivity = shouldOpenMainActivity,
                onStepChanged = onStepChanged,
                onShowMessage = onShowMessage,
            )
        }
        val oldPin: String? = tipBundle.oldPin
        val tipCounter = Session.getTipCounter()
        val deviceId = tipBundle.deviceId
        val nodeCounter = tipBundle.tipEvent?.nodeCounter ?: tipCounter
        val failedSigners = tipBundle.tipEvent?.failedSigners
        var nodeFailedInfo = ""
        val observer: Tip.Observer = object : Tip.Observer {
            override fun onSyncing(step: Int, total: Int) {
                lifecycleScope.launch {
                    onStepChanged(Processing.SyncingNode(step, total))
                }
            }
            override fun onSyncingComplete() {
                lifecycleScope.launch {
                    onStepChanged(Processing.Updating)
                }
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
            Timber.i("LoginFlow safe_register_start tip_type=${tipBundle.tipType}")
            val registerResult: Boolean = try {
                registerPublicKey(context, tipBundle, pin, tipPriv, onStepChanged, onShowMessage)
            } catch (e: Exception) {
                val errorInfo = e.getTipExceptionMsg(context)
                onStepChanged(RetryRegister(null, errorInfo))
                false
            }
            Timber.i("LoginFlow safe_register_result success=$registerResult has_safe=${Session.hasSafe()}")
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
            if (!openNextAfterPin(context, tipBundle.tipType == TipType.Create, pin)) {
                val reason = context.getString(R.string.Save_failure)
                tipBundle.pin = null
                tipBundle.oldPin = null
                onShowMessage(reason)
                onStepChanged(RetryRegister(null, reason))
                return false
            }
        }
        return true
    }

    private suspend fun openNextAfterPin(
        context: Context,
        skipImportPin: Boolean = false,
        pin: String,
    ): Boolean {
        val activity = context.findFragmentActivityOrNull()
        val syncedWallets = ensureClassicWallet(context, pin) ?: return false
        if (!hasPendingImportMnemonic(context)) {
            MainActivity.show(context)
            return true
        }
        val wallets = syncedWallets
        val resolution = resolvePendingMnemonicForWalletsOrNull(
            context = context,
            tip = tip,
            web3Repository = web3Repository,
            wallets = wallets,
            pin = pin,
            source = "tip",
        ) ?: return false
        Timber.i(
            "LoginFlow pending_import_wallet_sync_result source=tip resolution=$resolution wallet_count=${wallets.size}"
        )
        return when (resolution) {
            is PendingMnemonicResolution.WalletHome -> {
                Timber.e(
                    "LoginFlow pending_import_wallet_open source=tip wallet_id=${resolution.walletId} category=${resolution.walletCategory}"
                )
                val walletDestination = walletDestinationForWallet(
                    resolution.walletId,
                    resolution.walletCategory,
                )
                MainActivity.showWallet(context, walletDestination = walletDestination)
                true
            }
            PendingMnemonicResolution.ImportMnemonic -> {
                if (activity != null) {
                    val mode = if (skipImportPin) {
                        WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC
                    } else {
                        WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC
                    }
                    Timber.i("LoginFlow pending_import_fetch_open source=tip mode=$mode pin_reused=true")
                    WalletSecurityActivity.show(activity, mode, pin = pin)
                } else {
                    Timber.i("LoginFlow pending_import_fetch_open_failed source=tip reason=no_activity")
                    MainActivity.show(context)
                }
                true
            }
            PendingMnemonicResolution.NeedPin,
            PendingMnemonicResolution.LocalSaveFailed -> false
        }
    }

    internal suspend fun ensureClassicWallet(
        context: Context,
        pin: String,
    ) = runCatching {
        val refreshedWallets = ensureInitialClassicWallet(
            syncWallets = { web3Repository.syncWalletsFromRoute() },
            isClassicWallet = { wallet -> wallet.category == WalletCategory.CLASSIC.value },
            createClassicWallet = { classicIndex ->
                val spendKey = tip.getSpendKeyFromPin(context, pin)
                val walletRequest = buildClassicWalletRequest(web3Repository, spendKey, classicIndex)
                requestRouteAPI(
                    invokeNetwork = { web3Repository.createWallet(walletRequest) },
                    successBlock = { response ->
                        response.data?.let { wallet ->
                            web3Repository.insertWallet(wallet)
                            wallet.addresses?.takeIf { it.isNotEmpty() }?.let { addresses ->
                                web3Repository.insertAddressList(addresses)
                            }
                        }
                        Timber.i("LoginFlow classic_wallet_create_success index=$classicIndex")
                    },
                    failureBlock = { response ->
                        Timber.e("Failed to create classic wallet: ${response.errorCode} - ${response.errorDescription}")
                        false
                    },
                    exceptionBlock = { throwable ->
                        Timber.e(throwable, "Failed to create classic wallet")
                        true
                    },
                    requestSession = { userRepository.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
                )
            },
        )
        Timber.i(
            "LoginFlow classic_wallet_ensure_complete has_classic=${refreshedWallets?.any { it.category == WalletCategory.CLASSIC.value } == true} wallet_count=${refreshedWallets?.size ?: -1} initial_index=$INITIAL_CLASSIC_WALLET_INDEX"
        )
        refreshedWallets
    }.getOrElse { throwable ->
        Timber.i("LoginFlow classic_wallet_ensure_exception")
        Timber.e(throwable, "Failed to ensure classic wallet")
        null
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
                if (hasPendingImportMnemonicInMemory()) {
                    markPendingImportMnemonic(context)
                }
                if (Session.hasPhone()) {
                    try {
                        tip.removeLocalMnemonicIfSafeMatches(context, pin, tipPriv)
                    } catch (e: Exception) {
                        tipBundle.oldPin = null
                        onStepChanged(RetryRegister(tipPriv, e.getTipExceptionMsg(context)))
                        return false
                    }
                }
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
            val account = requireNotNull(registerResp.data) { "required account can not be null" }
            Session.storeAccount(account)
            if (hasPendingImportMnemonicInMemory()) {
                markPendingImportMnemonic(context)
            }
            if (Session.hasPhone()) {
                try {
                    tip.removeLocalMnemonicIfSafeMatches(context, pin, seed)
                } catch (e: Exception) {
                    tipBundle.oldPin = null
                    onStepChanged(RetryRegister(seed, e.getTipExceptionMsg(context)))
                    return false
                }
            }
            val solAddress: String = getTipAddress(context, pin, SOLANA_CHAIN_ID)
            val evmAddress: String = getTipAddress(context, pin, ETHEREUM_CHAIN_ID)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, solAddress)
            Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, evmAddress)
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

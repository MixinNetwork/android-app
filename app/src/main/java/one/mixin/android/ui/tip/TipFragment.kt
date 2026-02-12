package one.mixin.android.ui.tip

import android.annotation.SuppressLint
import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.crypto.removeValueFromEncryptedPreferences
import one.mixin.android.databinding.FragmentTipBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexString
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.Web3Repository
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.TipNotAllWatcherSuccessException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.privateKeyToAddress
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.Web3Signer
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.ECKey
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.time.Instant
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class TipFragment : BaseFragment(R.layout.fragment_tip) {
    companion object {
        const val TAG = "TipFragment"
        const val ARGS_TIP_BUNDLE = "args_tip_bundle"
        const val ARGS_SHOULD_WATCH = "args_should_watch"

        fun newInstance(
            tipBundle: TipBundle,
            shouldWatch: Boolean,
        ) =
            TipFragment().withArgs {
                putParcelable(ARGS_TIP_BUNDLE, tipBundle)
                putBoolean(ARGS_SHOULD_WATCH, shouldWatch)
            }
    }

    private val viewModel by viewModels<TipViewModel>()
    private val binding by viewBinding(FragmentTipBinding::bind)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var web3Repository: Web3Repository

    @Inject
    lateinit var jobManager: MixinJobManager

    private val tipBundle: TipBundle by lazy { requireArguments().getTipBundle() }

    private var nodeFailedInfo = ""

    private var disallowClose = true

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Timber.e("$TAG onViewCreated type: ${tipBundle.tipType}, event: ${tipBundle.tipEvent}")
        binding.apply {
            if (tipBundle.tipType == TipType.Create) {
                AnalyticsTracker.trackSignUpPinSet()
            }
            closeIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            support.setOnClickListener {
                context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }

            val tip1 = SpannableStringBuilder(getString(R.string.Please_use_when_network_is_connected))
            val tip2 = SpannableStringBuilder(getString(R.string.Please_keep_app_in_foreground))
            val tip3 =
                SpannableStringBuilder()
                    .color(requireContext().getColor(R.color.colorRed)) {
                        append(getString(R.string.Process_can_not_be_stop))
                    }
            tipsTv.text = buildBulletLines(requireContext(), tip1, tip2, tip3)
            bottomHintTv.movementMethod = ScrollingMovementMethod()
            bottomHintTv.setTextIsSelectable(true)

            val fontScale = requireActivity().resources.configuration.fontScale
            if (fontScale >= 1.3) {
                bottomVa.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToTop = -1
                    topToBottom = tipsTv.id
                }
                bottomHintTv.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomToBottom = -1
                    topToBottom = bottomVa.id
                }
                logoIv.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    verticalBias = 0f
                }
            }

            when (tipBundle.tipType) {
                TipType.Create -> titleTv.setText(R.string.Create_PIN)
                TipType.Change -> titleTv.setText(R.string.Change_PIN)
                TipType.Upgrade -> titleTv.setText(R.string.Upgrade_TIP)
            }
        }

        when (tipBundle.tipStep) {
            Processing.Creating -> processTip()
            else -> tryConnect(requireArguments().getBoolean(ARGS_SHOULD_WATCH))
        }
    }

    override fun onBackPressed(): Boolean {
        return disallowClose
    }

    private fun updateTipStep(newVal: TipStep) {
        tipBundle.tipStep = newVal
        updateUI(newVal)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(tipStep: TipStep) {
        if (viewDestroyed()) return

        binding.apply {
            val forRecover = tipBundle.forRecover()
            updateAllowClose(tipStep, forRecover)
            title.setOnLongClickListener {
                LogViewerBottomSheet.newInstance().showNow(parentFragmentManager, LogViewerBottomSheet.TAG)
                true
            }
            when (tipStep) {
                is TryConnecting -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 1
                    bottomHintTv.text = getString(R.string.Trying_connect_tip_network)
                    bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                }
                is RetryConnect -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    innerTv.text = getString(R.string.Retry)
                    innerTv.setOnClickListener { tryConnect(tipStep.shouldWatch) }
                    bottomHintTv.text = (if (tipStep.reason.isBlank()) "" else "${tipStep.reason}\n") +
                        getString(R.string.Connect_to_TIP_network_failed)
                    bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
                }
                is ReadyStart -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    if (forRecover) {
                        innerTv.text = getString(R.string.Continue)
                        innerTv.setOnClickListener { recover() }
                    } else {
                        when (tipBundle.tipType) {
                            TipType.Upgrade -> {
                                innerTv.text = getString(R.string.Upgrade)
                                innerTv.setOnClickListener {
                                    showVerifyPin(getString(R.string.Enter_your_PIN)) { pin ->
                                        tipBundle.oldPin = pin // as legacy pin
                                        tipBundle.pin = pin
                                        processTip()
                                    }
                                }
                            }
                            else -> {
                                innerTv.text = getString(R.string.Start)
                                innerTv.setOnClickListener { start() }
                            }
                        }
                    }
                    bottomHintTv.text = ""
                }
                is RetryProcess -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    innerTv.text = getString(R.string.Retry)
                    innerTv.setOnClickListener {
                        if (tipBundle.pin.isNullOrBlank()) {
                            showInputPin { pin ->
                                tipBundle.pin = pin
                                processTip()
                            }
                        } else {
                            processTip()
                        }
                    }
                    bottomHintTv.text = tipStep.reason
                    bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
                }
                is Processing -> {
                    descTv.setText(R.string.Syncing_and_verifying_TIP)
                    tipsTv.isVisible = false
                    bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))

                    when (tipStep) {
                        is Processing.Creating -> {
                            bottomVa.displayedChild = 2
                            bottomHintTv.text = getString(R.string.Trying_connect_tip_node)
                        }
                        is Processing.SyncingNode -> {
                            bottomVa.displayedChild = 1
                            pb.max = tipStep.total
                            pb.progress = tipStep.step
                            val percent = ceil((tipStep.step / tipStep.total.toDouble()) * 100).toInt()
                            bottomHintTv.text = getString(R.string.Exchanging_data, percent.toString())
                        }
                        is Processing.Updating -> {
                            bottomVa.displayedChild = 2
                            bottomHintTv.text = getString(R.string.Generating_keys)
                        }
                        is Processing.Registering -> {
                            bottomVa.displayedChild = 2
                            bottomHintTv.text = getString(R.string.Registering)
                        }
                    }
                }
                is RetryRegister -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    innerTv.text = getString(R.string.Retry)
                    innerTv.setOnClickListener {
                        val pin = tipBundle.pin
                        if (pin.isNullOrBlank()) {
                            showInputPin { p ->
                                tipBundle.pin = p
                                onTipProcessSuccess(p, tipStep.tipPriv)
                            }
                        } else {
                            lifecycleScope.launch {
                                onTipProcessSuccess(pin, tipStep.tipPriv)
                            }
                        }
                    }
                    bottomHintTv.text = tipStep.reason
                    bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
                }
                is LegacyPIN -> {
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    innerTv.text = getString(R.string.View_Document)
                    innerTv.setOnClickListener {
                        context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                    }
                    bottomHintTv.text = tipStep.message
                    bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
                }
            }
        }
    }

    private fun updateAllowClose(
        tipStep: TipStep,
        forRecover: Boolean,
    ) {
        disallowClose =
            when (tipStep) {
                is TryConnecting, is RetryConnect, is ReadyStart -> {
                    forRecover || !tipBundle.forChange()
                }
                is RetryProcess, is Processing, is RetryRegister, is LegacyPIN -> {
                    true
                }
            }
        binding.closeIv.isVisible = !disallowClose
    }

    private fun tryConnect(shouldWatch: Boolean) {
        updateTipStep(TryConnecting)
        lifecycleScope.launch {
            var available = false
            var info = ""
            if (shouldWatch) {
                val tipCounter = Session.getTipCounter()
                tip.checkCounter(
                    tipCounter,
                    onNodeCounterNotEqualServer = { nodeMaxCounter, nodeFailedSigners ->
                        tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                    },
                    onNodeCounterInconsistency = { nodeMaxCounter, nodeFailedSigners ->
                        tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                    },
                ).onSuccess { available = true }
                    .onFailure {
                        Timber.d("try connect tip watch failure $it")
                        available = false
                        if (it is TipNotAllWatcherSuccessException) {
                            info = it.info
                        }
                    }
            } else {
                val pair = viewModel.checkTipNodeConnect()
                available = pair.first
                info = pair.second
            }

            if (available) {
                updateTipStep(ReadyStart)
            } else {
                updateTipStep(RetryConnect(shouldWatch, info))
            }
        }
    }

    private fun recover() {
        when (tipBundle.tipType) {
            TipType.Change -> {
                val tipCounter = Session.getTipCounter()
                val nodeCounter = tipBundle.tipEvent?.nodeCounter ?: tipCounter
                val failedSigners = tipBundle.tipEvent?.failedSigners
                if (nodeCounter != tipCounter && failedSigners?.size == tip.tipNodeCount()) {
                    // for fix tipCounter > nodeCounter
                    showInputPin(getString(R.string.Enter_your_PIN)) { pin ->
                        tipBundle.oldPin = pin
                        tipBundle.pin = pin
                        processTip()
                    }
                } else {
                    // We should always input old PIN to decrypt encryptedSalt
                    // even if there are no failed signers.
                    showVerifyPin { oldPin ->
                        tipBundle.oldPin = oldPin
                        showInputPin { pin ->
                            tipBundle.pin = pin
                            processTip()
                        }
                    }
                }
            }
            TipType.Upgrade -> {
                showVerifyPin(getString(R.string.Enter_your_PIN)) { pin ->
                    tipBundle.oldPin = pin // as legacy pin
                    tipBundle.pin = pin
                    processTip()
                }
            }
            TipType.Create -> {
                showInputPin(getString(R.string.Enter_your_PIN)) { pin ->
                    tipBundle.pin = pin
                    processTip()
                }
            }
        }
    }

    private fun start() {
        if (tipBundle.tipType == TipType.Upgrade) {
            Timber.w("Tip start in error state ${tipBundle.tipType}")
            return
        }
        val passwordFragment = WalletPasswordFragment.newInstance(tipBundle)
        navTo(passwordFragment, WalletPasswordFragment.TAG)
    }

    private fun processTip() =
        lifecycleScope.launch {
            updateTipStep(Processing.Creating)
            nodeFailedInfo = ""

            val tipCounter = Session.getTipCounter()
            val deviceId = tipBundle.deviceId
            val nodeCounter = tipBundle.tipEvent?.nodeCounter ?: tipCounter
            val failedSigners = tipBundle.tipEvent?.failedSigners
            val pin = requireNotNull(tipBundle.pin) { "process tip step pin can not be null" }
            val oldPin = tipBundle.oldPin
            Timber.e("tip nodeCounter $nodeCounter, tipCounter $tipCounter, signers size ${failedSigners?.size}")

            when(tipBundle.tipType) {
                TipType.Change -> {
                    AnalyticsTracker.trackLoginPinVerify("pin_change")
                }
                TipType.Upgrade -> {
                    AnalyticsTracker.trackLoginPinVerify("pin_upgrade")
                }
                else -> {
                    // do nothing
                }
            }

            tip.addObserver(tipObserver)
            when {
                tipCounter < 1 ->
                    tip.createTipPriv(requireContext(), pin, deviceId, failedSigners, oldPin)
                else ->
                    tip.updateTipPriv(requireContext(), deviceId, pin, requireNotNull(oldPin) { "process tip step update oldPin can not be null" }, nodeCounter == tipCounter, failedSigners)
            }.onFailure { e ->
                tip.removeObserver(tipObserver)
                onTipProcessFailure(e, pin, tipCounter, nodeCounter)
            }.onSuccess {
                tip.removeObserver(tipObserver)
                onTipProcessSuccess(pin, it)
            }
        }

    private suspend fun onTipProcessFailure(
        e: Throwable,
        pin: String,
        tipCounter: Int,
        nodeCounterBeforeRequest: Int,
    ) {
        val extraInfo = "account counter: $tipCounter, nodeCounterBeforeRequest: $nodeCounterBeforeRequest, type: ${tipBundle.tipType}, step: ${tipBundle.tipStep}, event: ${tipBundle.tipEvent}"
        val errMsg = e.getTipExceptionMsg(requireContext(), nodeFailedInfo, extraInfo)
        toast(errMsg)

        if (e is DifferentIdentityException) {
            tipBundle.oldPin = null
            tipBundle.pin = null
            updateTipStep(RetryConnect(true, ""))
            return
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
            updateTipStep(RetryConnect(true, errorInfo))
            return
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
            refreshAccount()
            val newSessionCounter = Session.getTipCounter()
            // If new session counter equals new node counter,
            // we consider this case as PIN update success.
            if (newNodeCounter == newSessionCounter) {
                onTipProcessSuccess(pin, null)
                return
            }
        }

        updateTipStep(RetryProcess(errMsg))
    }

    private suspend fun onTipProcessSuccess(
        pin: String,
        tipPriv: ByteArray?,
    ) {
        if (!Session.hasSafe()) {
            val registerResult: Boolean =
                try {
                    registerPublicKey(pin, tipPriv).getOrThrow()
                } catch (e: Exception) {
                    val errorInfo = e.getTipExceptionMsg(requireContext())
                    updateTipStep(RetryRegister(null, errorInfo))
                    false
                }
            if (!registerResult) {
                // register public key failed, already go to RetryRegister
                return
            }
        }

        val cur = System.currentTimeMillis()
        defaultSharedPreferences.putBoolean(PREF_LOGIN_OR_SIGN_UP, true)
        defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
        putPrefPinInterval(requireContext(), INTERVAL_10_MINS)

        val openBiometrics = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (openBiometrics) {
            try {
                BiometricUtil.savePin(requireContext(), pin)
            } catch (ignored: UserNotAuthenticatedException) {
                Timber.e("$TAG savePin $ignored")
            }
        }

        when (tipBundle.tipType) {
            TipType.Change -> toast(R.string.Change_PIN_successfully)
            TipType.Create -> toast(R.string.Set_PIN_successfully)
            TipType.Upgrade -> toast(R.string.Upgrade_TIP_successfully)
        }

        if (tipBundle.tipType == TipType.Create) {
            AnalyticsTracker.trackSignUpEnd()
        }
        if (activity?.isTaskRoot == true) {
            MainActivity.show(requireContext())
        }
        activity?.finish()
    }

    private suspend fun registerPublicKey(
        pin: String,
        tipPriv: ByteArray?,
    ): Result<Boolean> =
        kotlin.runCatching {
            Timber.e("$TAG start registerPublicKey")
            updateTipStep(Processing.Registering)

            val meResp = accountService.getMeSuspend()
            if (meResp.isSuccess) {
                val account = requireNotNull(meResp.data) { "required account can not be null" }
                Session.storeAccount(account)
                if (account.hasSafe) {
                    return@runCatching true
                }
            } else {
                tipBundle.oldPin = null
                val error = requireNotNull(meResp.error) { "error can not be null" }
                val errorInfo =
                    requireContext().getMixinErrorStringByCode(error.code, error.description)
                updateTipStep(RetryRegister(tipPriv, errorInfo))
                return@runCatching false
            }

            nodeFailedInfo = ""
            val seed =
                try {
                    tipPriv ?: tip.getOrRecoverTipPriv(requireContext(), pin).getOrThrow()
                } catch (e: Exception) {
                    tipBundle.oldPin = null
                    val errorInfo = e.getTipExceptionMsg(requireContext())
                    updateTipStep(RetryRegister(null, errorInfo))
                    return@runCatching false
                }
            val spendSeed = tip.getSpendPriv(requireContext(), seed)
            val saltBase64 = tip.getEncryptSalt(this.requireContext(), pin, seed, Session.isAnonymous())
            val spendKeyPair = newKeyPairFromSeed(spendSeed)
            val selfAccountId = requireNotNull(Session.getAccountId()) { "self userId can not be null at this step" }
            val edKey = tip.getMnemonicEdKey(requireContext(), pin, seed)
            val registerResp =
                viewModel.registerPublicKey(
                    registerRequest =
                        RegisterRequest(
                            publicKey = spendKeyPair.publicKey.toHex(),
                            signature = Session.getRegisterSignature(selfAccountId, spendSeed),
                            pin = viewModel.getEncryptedTipBody(selfAccountId, spendKeyPair.publicKey.toHex(), pin),
                            salt = saltBase64,
                            masterPublicHex = edKey.publicKey.hexString(),
                            masterSignatureHex = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), selfAccountId.toByteArray()).hexString()
                        ),
                )
            return@runCatching if (registerResp.isSuccess) {
                val solAddress = viewModel.getTipAddress(requireContext(), pin, SOLANA_CHAIN_ID)
                PropertyHelper.updateKeyValue(SOLANA_ADDRESS, solAddress)
                val evmAddress = viewModel.getTipAddress(requireContext(), pin, ETHEREUM_CHAIN_ID)
                PropertyHelper.updateKeyValue(EVM_ADDRESS, evmAddress)
                Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Solana.name, solAddress)
                Web3Signer.updateAddress(Web3Signer.JsSignerNetwork.Ethereum.name, evmAddress)
                Session.storeAccount(requireNotNull(registerResp.data) { "required account can not be null" })
                createWallet(spendSeed)
                if (Session.hasPhone()) { // Only clear Phone user
                    removeValueFromEncryptedPreferences(requireContext(), Constants.Tip.MNEMONIC)
                }
                true
            } else if (registerResp.errorCode == ErrorHandler.INVALID_PIN_FORMAT) {
                tipBundle.oldPin = null
                updateTipStep(LegacyPIN(getString(R.string.error_legacy_pin)))
                false
            } else {
                tipBundle.oldPin = null
                val error = requireNotNull(registerResp.error) { "error can not be null" }
                val errorInfo =
                    requireContext().getMixinErrorStringByCode(error.code, error.description)
                updateTipStep(RetryRegister(tipPriv, errorInfo))
                false
            }
        }

    private suspend fun createWallet(spendKey: ByteArray) {
        val hasClassicWallet: Boolean = web3Repository.getClassicWalletId() != null
        if (hasClassicWallet) {
            return
        }
        val walletName: String = requireContext().getString(R.string.Common_Wallet)
        val classicIndex = 0
        val btcAddress: String = privateKeyToAddress(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex)
        val evmAddress: String = privateKeyToAddress(spendKey, ETHEREUM_CHAIN_ID, classicIndex)
        val solAddress: String = privateKeyToAddress(spendKey, SOLANA_CHAIN_ID, classicIndex)
        val addresses: List<Web3AddressRequest> = listOf(
            createSignedWeb3AddressRequest(
                destination = evmAddress,
                chainId = ETHEREUM_CHAIN_ID,
                path = Bip44Path.ethereumPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, ETHEREUM_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value
            ),
            createSignedWeb3AddressRequest(
                destination = solAddress,
                chainId = SOLANA_CHAIN_ID,
                path = Bip44Path.solanaPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, SOLANA_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value
            ),
            createSignedWeb3AddressRequest(
                destination = btcAddress,
                chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                path = Bip44Path.bitcoinSegwitPathString(classicIndex),
                privateKey = tipPrivToPrivateKey(spendKey, Constants.ChainId.BITCOIN_CHAIN_ID, classicIndex),
                category = WalletCategory.CLASSIC.value
            )
        )
        val walletRequest = WalletRequest(
            name = walletName,
            category = WalletCategory.CLASSIC.value,
            addresses = addresses
        )
        requestRouteAPI(
            invokeNetwork = { web3Repository.createWallet(walletRequest) },
            successBlock = { response ->
                response.data?.let { wallet ->
                    web3Repository.insertWallet(
                        Web3Wallet(
                            id = wallet.id,
                            name = wallet.name,
                            category = wallet.category,
                            createdAt = wallet.createdAt,
                            updatedAt = wallet.updatedAt,
                        )
                    )
                    val walletAddresses = wallet.addresses ?: emptyList()
                    if (walletAddresses.isNotEmpty()) {
                        web3Repository.insertAddressList(walletAddresses)
                        MixinApplication.appContext.defaultSharedPreferences.putBoolean(Constants.Account.PREF_WEB3_ADDRESSES_SYNCED, true)
                    }
                }
            },
            requestSession = { ids ->
                web3Repository.fetchSessionsSuspend(ids)
            }
        )
    }

    private fun createSignedWeb3AddressRequest(
        destination: String,
        chainId: String,
        path: String?,
        privateKey: ByteArray,
        category: String
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
        val message = "$destination\n$selfId\n${now.epochSecond}"
        val signature =
            if (chainId == SOLANA_CHAIN_ID) {
                Numeric.prependHexPrefix(Web3Signer.signSolanaMessage(privateKey, message.toByteArray()))
            } else if (chainId in Constants.Web3EvmChainIds) {
                Web3Signer.signEthMessage(privateKey, message.toByteArray().toHexString(), JsSignMessage.TYPE_PERSONAL_MESSAGE)
            } else if (chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                val ecKey: ECKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
                Numeric.toHexString(ecKey.signMessage(message, ScriptType.P2WPKH).decodeBase64())
            } else {
                null
            }

        return Web3AddressRequest(
            destination = destination,
            chainId = chainId,
            path = path,
            signature = signature,
            timestamp = now.toString()
        )
    }

    private fun showVerifyPin(
        title: String? = null,
        onVerifySuccess: suspend (String) -> Unit,
    ) {
        Timber.e("$TAG showVerifyPin")
        VerifyBottomSheetDialogFragment.newInstance(title ?: getString(R.string.Enter_your_old_PIN), true).setOnPinSuccess { pin ->
            lifecycleScope.launch {
                onVerifySuccess(pin)
            }
        }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
    }

    private fun showInputPin(
        title: String? = null,
        onInputComplete: suspend (String) -> Unit,
    ) {
        Timber.e("$TAG showInputPin")
        PinInputBottomSheetDialogFragment.newInstance(title ?: getString(R.string.Enter_your_new_PIN)).setOnPinComplete { pin ->
            lifecycleScope.launch {
                onInputComplete(pin)
            }
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun setTitle(forRecover: Boolean) {
        binding.apply {
            if (forRecover) {
                descTv.text =
                    getString(
                        when (tipBundle.tipType) {
                            TipType.Create -> R.string.Creating_wallet_terminated_unexpectedly
                            TipType.Upgrade -> R.string.Upgrading_TIP_terminated_unexpectedly
                            TipType.Change -> R.string.Changing_PIN_terminated_unexpectedly
                        },
                    )
            } else {
                if (tipBundle.forCreate()) {
                    descTv.highlightStarTag(getString(R.string.TIP_creation_introduction), arrayOf(Constants.HelpLink.TIP))
                } else {
                    descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                }
            }
        }
    }

    private suspend fun refreshAccount() {
        handleMixinResponse(
            invokeNetwork = { accountService.getMeSuspend() },
            successBlock = { r ->
                r.data?.let { Session.storeAccount(it) }
            },
        )
    }

    private val tipObserver =
        object : Tip.Observer {
            override fun onSyncing(
                step: Int,
                total: Int,
            ) {
                lifecycleScope.launch {
                    updateTipStep(Processing.SyncingNode(step, total))
                }
            }

            override fun onSyncingComplete() {
                lifecycleScope.launch {
                    updateTipStep(Processing.Updating)
                }
            }

            override fun onNodeFailed(info: String) {
                nodeFailedInfo = info
            }
        }
}

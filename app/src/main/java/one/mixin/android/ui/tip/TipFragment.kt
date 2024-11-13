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
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RegisterRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.crypto.clearMnemonic
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.databinding.FragmentTipBinding
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexString
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putLong
import one.mixin.android.extension.toHex
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.exception.DifferentIdentityException
import one.mixin.android.tip.exception.NotAllSignerSuccessException
import one.mixin.android.tip.exception.TipNotAllWatcherSuccessException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import timber.log.Timber
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

    private val tipBundle: TipBundle by lazy { requireArguments().getTipBundle() }

    private var nodeFailedInfo = ""

    private var disallowClose = true

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            closeIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
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
                is RetryProcess, is Processing, is RetryRegister -> {
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
                    showInputPin(getString(R.string.Enter_your_old_PIN)) { oldPin ->
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
            Timber.d("tip nodeCounter $nodeCounter, tipCounter $tipCounter, signers size ${failedSigners?.size}")

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
        val errMsg = e.getTipExceptionMsg(requireContext(), nodeFailedInfo)
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

        activity?.finish()
    }

    private suspend fun registerPublicKey(
        pin: String,
        tipPriv: ByteArray?,
    ): Result<Boolean> =
        kotlin.runCatching {
            Timber.d("Tip start registerPublicKey")
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
            val masterKey = tip.getMasterKey(this.requireContext())
            val salt = masterKey.privKeyBytes
            val saltBase64 = tip.getEncryptSalt(this.requireContext(), pin, seed)
            val spendSeed = tip.getSpendPriv(seed, salt)
            val keyPair = newKeyPairFromSeed(spendSeed)
            val pkHex = keyPair.publicKey.toHex()
            val selfId = requireNotNull(Session.getAccountId()) { "self userId can not be null at this step" }
            val edKey = tip.getMnemonicEdKey(requireContext())
            val registerResp =
                viewModel.registerPublicKey(
                    registerRequest =
                        RegisterRequest(
                            publicKey = pkHex,
                            signature = Session.getRegisterSignature(selfId, spendSeed),
                            pin = viewModel.getEncryptedTipBody(selfId, pkHex, pin),
                            salt = saltBase64,
                            saltPublicHex = edKey.publicKey.hexString(),
                            saltSignatureHex = initFromSeedAndSign(edKey.privateKey.toTypedArray().toByteArray(), selfId.toByteArray()).hexString()
                        ),
                )
            return@runCatching if (registerResp.isSuccess) {
                Session.storeAccount(requireNotNull(registerResp.data) { "required account can not be null" })
                if (Session.hasPhone()) { // Only clear Phone user
                    clearMnemonic(requireContext(), Constants.Tip.MNEMONIC)
                }
                true
            } else {
                tipBundle.oldPin = null
                val error = requireNotNull(registerResp.error) { "error can not be null" }
                val errorInfo =
                    requireContext().getMixinErrorStringByCode(error.code, error.description)
                updateTipStep(RetryRegister(tipPriv, errorInfo))
                false
            }
        }

    private fun showVerifyPin(
        title: String? = null,
        onVerifySuccess: suspend (String) -> Unit,
    ) {
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

package one.mixin.android.ui.tip

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.view.View
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.INTERVAL_10_MINS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.PrivacyPreference.putPrefPinInterval
import one.mixin.android.databinding.FragmentTipBinding
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putLong
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
import one.mixin.android.util.viewBinding
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class TipFragment : BaseFragment(R.layout.fragment_tip) {
    companion object {
        const val TAG = "TipFragment"
        const val ARGS_TIP_BUNDLE = "args_tip_bundle"

        fun newInstance(tipBundle: TipBundle) = TipFragment().withArgs {
            putParcelable(ARGS_TIP_BUNDLE, tipBundle)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            closeIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            val tip1 = SpannableStringBuilder(getString(R.string.Please_use_when_network_is_connected))
            val tip2 = SpannableStringBuilder(getString(R.string.Please_keep_app_in_foreground))
            val tip3 = SpannableStringBuilder()
                .color(requireContext().getColor(R.color.colorRed)) {
                    append(getString(R.string.Process_can_not_be_stop))
                }
            tipsTv.text = buildBulletLines(requireContext(), tip1, tip2, tip3)
            bottomHintTv.movementMethod = ScrollingMovementMethod()
            bottomHintTv.setTextIsSelectable(true)

            when (tipBundle.tipType) {
                TipType.Create -> titleTv.setText(R.string.Create_PIN)
                TipType.Change -> titleTv.setText(R.string.Change_PIN)
                TipType.Upgrade -> titleTv.setText(R.string.Upgrade_TIP)
            }
        }

        when (tipBundle.tipStep) {
            Processing.Creating -> processTip()
            else -> tryConnect(false)
        }
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
            when (tipStep) {
                is TryConnecting -> {
                    closeIv.isVisible = true
                    setTitle(forRecover)
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 1
                    bottomHintTv.text = getString(R.string.Trying_connect_tip_network)
                    bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
                is RetryConnect -> {
                    closeIv.isVisible = true
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
                    closeIv.isVisible = true
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
                    closeIv.isVisible = false
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
                    closeIv.isVisible = false
                    descTv.setText(R.string.Syncing_and_verifying_TIP)
                    tipsTv.isVisible = false
                    bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))

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
                    }
                }
            }
        }
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
                    onNodeCounterGreaterThanServer = { tipBundle.updateTipEvent(null, it) },
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
                val nodeSuccess = nodeCounter > tipCounter && failedSigners.isNullOrEmpty()
                if (nodeSuccess) {
                    showInputPin { pin ->
                        tipBundle.pin = pin
                        processTip()
                    }
                } else {
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

    private fun processTip() = lifecycleScope.launch {
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
            nodeCounter > tipCounter && failedSigners.isNullOrEmpty() ->
                tip.updateTipPriv(requireContext(), deviceId, pin, null, null)
            else ->
                tip.updateTipPriv(requireContext(), deviceId, pin, requireNotNull(oldPin), failedSigners)
        }.onFailure { e ->
            tip.removeObserver(tipObserver)
            onTipProcessFailure(e, pin, tipCounter, nodeCounter)
        }.onSuccess {
            tip.removeObserver(tipObserver)
            onTipProcessSuccess(pin)
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
            onNodeCounterGreaterThanServer = { tipBundle.updateTipEvent(null, it) },
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
                onTipProcessSuccess(pin)
                return
            }
        }

        updateTipStep(RetryProcess(errMsg))
    }

    private fun onTipProcessSuccess(pin: String) {
        val cur = System.currentTimeMillis()
        defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
        putPrefPinInterval(requireContext(), INTERVAL_10_MINS)

        val openBiometrics = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (openBiometrics) {
            BiometricUtil.savePin(requireContext(), pin, this)
        }

        when (tipBundle.tipType) {
            TipType.Change -> toast(R.string.Change_PIN_successfully)
            TipType.Create -> toast(R.string.Set_PIN_successfully)
            TipType.Upgrade -> toast(R.string.Upgrade_TIP_successfully)
        }

        activity?.finish()
    }

    private fun showVerifyPin(title: String? = null, onVerifySuccess: (String) -> Unit) {
        VerifyBottomSheetDialogFragment.newInstance(title ?: getString(R.string.Enter_your_old_PIN), true).setOnPinSuccess { pin ->
            onVerifySuccess(pin)
        }.apply {
            autoDismiss = true
        }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
    }

    private fun showInputPin(title: String? = null, onInputComplete: (String) -> Unit) {
        PinInputBottomSheetDialogFragment.newInstance(title ?: getString(R.string.Enter_your_new_PIN)).setOnPinComplete { pin ->
            onInputComplete(pin)
        }.showNow(parentFragmentManager, PinInputBottomSheetDialogFragment.TAG)
    }

    private fun setTitle(forRecover: Boolean) {
        binding.apply {
            if (forRecover) {
                descTv.text = getString(
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

    private val tipObserver = object : Tip.Observer {
        override fun onSyncing(step: Int, total: Int) {
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

package one.mixin.android.ui.tip

import android.os.Bundle
import android.text.SpannableStringBuilder
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
import one.mixin.android.tip.DifferentIdentityException
import one.mixin.android.tip.NotAllSignerSuccessException
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipNodeException
import one.mixin.android.tip.checkCounter
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.setting.OldPasswordFragment
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding
import timber.log.Timber
import javax.inject.Inject

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

    private val tipBundle: TipBundle by lazy { requireArguments().getTipBundle() }

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

            when (tipBundle.tipType) {
                TipType.Create -> titleTv.setText(R.string.Create_PIN)
                TipType.Change -> titleTv.setText(R.string.Change_PIN)
                TipType.Upgrade -> titleTv.setText(R.string.Upgrade_PIN)
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

    private fun updateUI(tipStep: TipStep) {
        if (viewDestroyed()) return

        binding.apply {
            val forRecover = tipBundle.forRecover()
            when (tipStep) {
                is TryConnecting -> {
                    closeIv.isVisible = true
                    if (forRecover) {
                        descTv.text = getString(R.string.Upgrade_PIN_aborted_unexpectedly_at, "2022")
                    } else {
                        descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                    }
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 1
                    bottomHintTv.text = getString(R.string.Trying_connect_tip_network)
                    bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
                is RetryConnect -> {
                    closeIv.isVisible = true
                    if (forRecover) {
                        descTv.text = getString(R.string.Upgrade_PIN_aborted_unexpectedly_at, "2022")
                    } else {
                        descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                    }
                    tipsTv.isVisible = true
                    bottomVa.displayedChild = 0
                    innerVa.displayedChild = 0
                    innerTv.text = getString(R.string.Retry)
                    innerTv.setOnClickListener { tryConnect(tipStep.shouldWatch) }
                    bottomHintTv.text = getString(R.string.Connect_to_TIP_network_failed)
                    bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
                }
                is ReadyStart -> {
                    closeIv.isVisible = true
                    if (forRecover) {
                        descTv.text = getString(R.string.Upgrade_PIN_aborted_unexpectedly_at, "2022")
                    } else {
                        descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                    }
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
                    if (forRecover) {
                        descTv.text = getString(R.string.Upgrade_PIN_aborted_unexpectedly_at, "2022")
                    } else {
                        descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                    }
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
                            bottomHintTv.text = getString(R.string.Exchanging_data, tipStep.step, tipStep.total)
                        }
                        is Processing.Updating -> {
                            bottomVa.displayedChild = 2
                            bottomHintTv.text = getString(R.string.Upgrading)
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
            if (shouldWatch) {
                val tipCounter = Session.getTipCounter()
                kotlin.runCatching {
                    tip.checkCounter(
                        tipCounter,
                        onNodeCounterGreaterThanServer = { tipBundle.updateTipEvent(null, it) },
                        onNodeCounterInconsistency = { nodeMaxCounter, nodeFailedSigners ->
                            tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                        }
                    )
                }.onSuccess { available = true }
                    .onFailure {
                        Timber.d("try connect tip watch failure $it")
                        available = false
                    }
            } else {
                available = viewModel.checkTipNodeConnect()
            }

            if (available) {
                updateTipStep(ReadyStart)
            } else {
                updateTipStep(RetryConnect(shouldWatch))
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
        when (tipBundle.tipType) {
            TipType.Change -> {
                val oldPasswordFragment = OldPasswordFragment.newInstance(tipBundle)
                navTo(oldPasswordFragment, OldPasswordFragment.TAG)
            }
            else -> {
                val passwordFragment = WalletPasswordFragment.newInstance(tipBundle)
                navTo(passwordFragment, WalletPasswordFragment.TAG)
            }
        }
    }

    private fun processTip() = lifecycleScope.launch {
        updateTipStep(Processing.Creating)

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
            onTipProcessFailure(e, tipCounter, nodeCounter)
        }.onSuccess {
            tip.removeObserver(tipObserver)
            onTipProcessSuccess(pin)
        }
    }

    private suspend fun onTipProcessFailure(
        e: Throwable,
        tipCounter: Int,
        nodeCounter: Int
    ) {
        val errMsg = e.getTipExceptionMsg(requireContext())
        toast(errMsg)

        if (e is TipNodeException) {
            if ((e is DifferentIdentityException) ||
                (e is NotAllSignerSuccessException && e.successSignerSize == 0) // all signer failed perhaps means PIN incorrect
            ) {
                tipBundle.pin = null
            }

            kotlin.runCatching {
                tip.checkCounter(
                    tipCounter,
                    onNodeCounterGreaterThanServer = { tipBundle.updateTipEvent(null, it) },
                    onNodeCounterInconsistency = { nodeMaxCounter, nodeFailedSigners ->
                        tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                    }
                )
            }.onFailure {
                // Generally, check-counter should NOT meet exceptions, if this happens,
                // we should go to the RetryConnect step to check network and other steps.
                tipBundle.pin = null
                tipBundle.oldPin = null
                updateTipStep(RetryConnect(true))
                return
            }

            updateTipStep(RetryProcess(errMsg))
        } else {
            // NOT TipNodeException means the nodes part must success,
            // clear failed node list to prepare for a new try to communicate with API server.
            tipBundle.updateTipEvent(null, nodeCounter)
            updateTipStep(RetryProcess(errMsg))
        }
    }

    private fun onTipProcessSuccess(pin: String) {
        val cur = System.currentTimeMillis()
        defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_CHECK, cur)
        putPrefPinInterval(requireContext(), INTERVAL_10_MINS)

        val openBiometrics = defaultSharedPreferences.getBoolean(Constants.Account.PREF_BIOMETRICS, false)
        if (openBiometrics) {
            BiometricUtil.savePin(requireContext(), pin, this)
        }

        if (tipBundle.forChange()) {
            toast(R.string.Change_PIN_successfully)
        } else {
            toast(R.string.Set_PIN_successfully)
        }

        // TODO go somewhere after set PIN
        activity?.finish()
    }

    private fun showVerifyPin(title: String? = null, onVerifySuccess: (String) -> Unit) {
        VerifyBottomSheetDialogFragment.newInstance(title ?: getString(R.string.Enter_your_old_PIN)).setOnPinSuccess { pin ->
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
    }
}

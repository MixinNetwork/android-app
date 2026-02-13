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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTipBinding
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.PinInputBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.logs.LogViewerBottomSheet
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.util.analytics.AnalyticsTracker
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

    private val binding by viewBinding(FragmentTipBinding::bind)

    @Inject
    lateinit var tip: Tip

    @Inject
    lateinit var tipFlowInteractor: TipFlowInteractor

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
                                processTip()
                            }
                        } else {
                            processTip()
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
        lifecycleScope.launch {
            tipFlowInteractor.tryConnect(
                context = requireContext(),
                tipBundle = tipBundle,
                shouldWatch = shouldWatch,
                onStepChanged = { step ->
                    updateTipStep(step)
                },
            )
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
                    showVerifyPin(getString(R.string.Enter_your_old_PIN)) { oldPin ->
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
            when (tipBundle.tipType) {
                TipType.Change -> AnalyticsTracker.trackLoginPinVerify("pin_change")
                TipType.Upgrade -> AnalyticsTracker.trackLoginPinVerify("pin_upgrade")
                else -> Unit
            }
            val success: Boolean = tipFlowInteractor.process(
                context = requireContext(),
                tipBundle = tipBundle,
                shouldOpenMainActivity = activity?.isTaskRoot == true,
                onStepChanged = { step ->
                    updateTipStep(step)
                },
                onShowMessage = { message ->
                    toast(message)
                },
            )
            if (!success) {
                return@launch
            }
            when (tipBundle.tipType) {
                TipType.Change -> toast(R.string.Change_PIN_successfully)
                TipType.Create -> toast(R.string.Set_PIN_successfully)
                TipType.Upgrade -> toast(R.string.Upgrade_TIP_successfully)
            }
            if (tipBundle.tipType == TipType.Create) {
                AnalyticsTracker.trackSignUpEnd()
            }
            activity?.finish()
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

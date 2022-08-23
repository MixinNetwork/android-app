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
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.tip.Tip
import one.mixin.android.tip.checkCounter
import one.mixin.android.tip.handleTipException
import one.mixin.android.ui.common.BaseFragment
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
            SyncingNode -> {
                updateTipStep(SyncingNode)
                processTip()
            }
            else -> tryConnect()
        }
    }

    private fun updateTipStep(newVal: TipStep) {
        tipBundle.tipStep = newVal
        updateUI(newVal)
    }

    private fun updateUI(newVal: TipStep) = binding.apply {
        when (newVal) {
            is TryConnecting -> {
                closeIv.isVisible = true
                descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                tipsTv.isVisible = true
                bottomVa.displayedChild = 0
                innerVa.displayedChild = 1
                bottomHintTv.text = getString(R.string.Trying_connect_tip_network)
                bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            }
            is RetryConnect -> {
                closeIv.isVisible = true
                descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                tipsTv.isVisible = true
                bottomVa.displayedChild = 0
                innerVa.displayedChild = 0
                innerTv.text = getString(R.string.Retry)
                innerTv.setOnClickListener { tryConnect() }
                bottomHintTv.text = getString(R.string.Connect_to_TIP_network_failed)
                bottomHintTv.setTextColor(requireContext().getColor(R.color.colorRed))
            }
            is ReadyStart -> {
                closeIv.isVisible = true
                descTv.highlightStarTag(getString(R.string.TIP_introduction), arrayOf(Constants.HelpLink.TIP))
                tipsTv.isVisible = true
                bottomVa.displayedChild = 0
                innerVa.displayedChild = 0
                when (tipBundle.tipType) {
                    TipType.Upgrade -> {
                        innerTv.text = getString(R.string.Upgrade)
                        innerTv.setOnClickListener { upgrade() }
                    }
                    else -> {
                        innerTv.text = getString(R.string.Start)
                        innerTv.setOnClickListener { start() }
                    }
                }
                bottomHintTv.text = ""
            }
            is SyncingNode -> {
                closeIv.isVisible = false
                descTv.setText(R.string.Syncing_and_verifying_TIP)
                tipsTv.isVisible = false
                bottomVa.displayedChild = 2
                bottomHintTv.text = getString(R.string.Trying_connect_tip_node)
                bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            }
            is ExchangeData -> {
                closeIv.isVisible = false
                descTv.setText(R.string.Syncing_and_verifying_TIP)
                tipsTv.isVisible = false
                bottomVa.displayedChild = 1
                bottomHintTv.text = getString(R.string.Exchanging_data, 1, 12)
                bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            }
            is FromRecover -> {
                closeIv.isVisible = true
                descTv.text = getString(R.string.Upgrade_PIN_aborted_unexpectedly_at, "2022")
                tipsTv.isVisible = true
                bottomVa.displayedChild = 0
                innerVa.displayedChild = 0
                innerTv.text = getString(R.string.Continue)
                bottomHintTv.text = getString(R.string.Continue_need_verify_your_PIN)
                bottomHintTv.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            }
        }
    }

    private fun tryConnect() {
        updateTipStep(TryConnecting)
        lifecycleScope.launch {
            val available = viewModel.checkTipNodeConnect()
            if (available) {
                updateTipStep(ReadyStart)
            } else {
                updateTipStep(RetryConnect)
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

    private fun upgrade() {
        val deviceId = defaultSharedPreferences.getString(Constants.DEVICE_ID, null)
        if (deviceId == null) {
            toast(R.string.Data_error)
            return
        }
        VerifyBottomSheetDialogFragment.newInstance().setOnPinSuccess { pin ->
            lifecycleScope.launch {
                tip.createTipPriv(requireContext(), pin, deviceId, tipBundle.tipEvent?.failedSigners)
            }
        }.apply {
            autoDismiss = true
            showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
        }
    }

    private fun processTip() = lifecycleScope.launch {
        val tipCounter = Session.getTipCounter()
        val deviceId = tipBundle.deviceId
        val nodeCounter = tipBundle.tipEvent?.nodeCounter ?: 0
        val failedSigners = tipBundle.tipEvent?.failedSigners
        val pin = requireNotNull(tipBundle.pin) { "process tip step pin can not be null" }
        val oldPin = tipBundle.oldPin
        Timber.d("tip nodeCounter $nodeCounter, tipCounter $tipCounter, signers size ${failedSigners?.size}")
        try {
            tip.addObserver(tipObserver)

            val tipPriv = if (tipCounter < 1) {
                tip.createTipPriv(requireContext(), pin, deviceId, failedSigners, oldPin)
            } else {
                val nodeSuccess = nodeCounter > tipCounter && failedSigners.isNullOrEmpty()
                tip.updateTipPriv(requireContext(), requireNotNull(oldPin), deviceId, pin, nodeSuccess, failedSigners)
            }.getOrNull()

            tip.removeObserver(tipObserver)

            if (tipPriv != null) {
                afterPinSuccess(pin)
            } else {
                // no exception happen means the nodes part must success,
                // clear failed node list to prepare for a new try.
                tipBundle.updateTipEvent(null, nodeCounter)
            }
        } catch (e: Exception) {
            e.handleTipException()

            tip.removeObserver(tipObserver)

            tip.checkCounter(
                tipCounter,
                onNodeCounterGreaterThanServer = { tipBundle.updateTipEvent(null, it) },
                onNodeCounterNotConsistency = { nodeMaxCounter, nodeFailedSigners ->
                    tipBundle.updateTipEvent(nodeFailedSigners, nodeMaxCounter)
                }
            )
        }
    }

    private fun afterPinSuccess(pin: String) {
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

    private val tipObserver = object : Tip.Observer {
        override fun onTipNodeComplete() {
            updateTipStep(ExchangeData)
        }
    }
}

package one.mixin.android.ui.setting

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentWalletPasswordBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.Processing
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipFragment
import one.mixin.android.ui.tip.TipFragment.Companion.ARGS_TIP_BUNDLE
import one.mixin.android.ui.tip.getTipBundle
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class WalletPasswordFragment : BaseFragment(R.layout.fragment_wallet_password), PinView.OnPinListener, PinView.onPinFinishListener {

    companion object {
        const val TAG = "WalletPasswordFragment"

        private const val STEP0 = 0
        private const val STEP1 = 1
        private const val STEP2 = 2
        private const val STEP3 = 3
        private const val STEP4 = 4

        fun newInstance(tipBundle: TipBundle): WalletPasswordFragment {
            return WalletPasswordFragment().withArgs {
                putParcelable(ARGS_TIP_BUNDLE, tipBundle)
            }
        }
    }

    private val binding by viewBinding(FragmentWalletPasswordBinding::bind)

    private var step = STEP0
    private var max = 5
    private var lastPassword: String? = null

    private val tipBundle: TipBundle by lazy { requireArguments().getTipBundle() }

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.apply {
            if (tipBundle.forChange()) {
                step = STEP0
                titleView.setSubTitle(getString(R.string.Old_PIN), "1/5")
                tipTv.text = getString(R.string.wallet_password_change_tip)
                max = 5
                titleView.initProgress(max, 0)
            } else {
                step = STEP1
                titleView.setSubTitle(getString(R.string.Set_PIN), "1/4")
                tipTv.text = getString(R.string.tip_create_pin_title)
                max = 4
                titleView.initProgress(max, 0)
            }
            titleView.leftIb.setOnClickListener {
                when (step) {
                    STEP1 -> activity?.onBackPressedDispatcher?.onBackPressed()
                    STEP2 -> toStep1()
                    STEP3 -> toStep2()
                    STEP4 -> toStep3()
                }
            }
            disableTitleRight()
            titleView.rightAnimator.setOnClickListener { createPin() }
            pin.setListener(this@WalletPasswordFragment)
            pin.setOnPinFinishListener(this@WalletPasswordFragment)
            keyboard.setKeyboardKeys(Constants.KEYS)
            keyboard.setOnClickKeyboardListener(keyboardListener)
            keyboard.animate().translationY(0f).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onUpdate(index: Int) {
        if (index == binding.pin.getCount()) {
            binding.titleView.rightTv.setTextColor(resources.getColor(R.color.colorBlue, null))
            binding.titleView.rightAnimator.isEnabled = true
        } else {
            disableTitleRight()
        }
    }

    override fun onPinFinish() {
        createPin()
    }

    override fun onBackPressed(): Boolean {
        when (step) {
            STEP0 -> return false
            STEP1 -> {
                return if (tipBundle.forChange()) {
                    toStep0()
                    true
                } else {
                    false
                }
            }
            STEP2 -> {
                toStep1()
                return true
            }
            STEP3 -> {
                toStep2()
                return true
            }
            STEP4 -> {
                toStep3()
                return true
            }
            else -> return false
        }
    }

    private fun disableTitleRight() {
        binding.titleView.apply {
            rightTv.setTextColor(resources.getColor(R.color.text_gray, null))
            rightAnimator.isEnabled = false
        }
    }

    private fun toStep0() {
        step = STEP0
        lastPassword = null
        binding.pin.clear()
        binding.titleView.setProgress(max - 5)
        binding.titleView.setSubTitle(
            getString(R.string.Old_PIN),
            getSubTitle()
        )
        binding.tipTv.text = getString(R.string.wallet_password_change_tip)
    }

    private fun toStep1() {
        step = STEP1
        lastPassword = null
        binding.pin.clear()
        binding.titleView.setProgress(max - 4)
        binding.titleView.setSubTitle(
            getString(if (tipBundle.forChange()) R.string.Set_new_PIN else R.string.Set_PIN),
            getSubTitle()
        )
        if (tipBundle.forChange()) {
            binding.tipTv.text = getString(R.string.wallet_password_set_new_pin_desc)
        } else {
            binding.tipTv.text = getString(R.string.tip_create_pin_title)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun toStep2(check: Boolean = false) {
        if (check && !validatePin()) {
            binding.pin.clear()
            return
        }

        step = STEP2
        binding.titleView.setProgress(max - 3)
        lastPassword = binding.pin.code()
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = "${getString(R.string.pin_confirm_hint)}\n${getString(R.string.pin_lost_hint)}"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun toStep3(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP3
        binding.titleView.setProgress(max - 2)
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = "${getString(R.string.pin_confirm_again_hint)}\n${getString(R.string.third_pin_confirm_hint)}"
        }
    }

    private fun toStep4(check: Boolean = false) {
        if (check && checkEqual()) return

        step = STEP4
        binding.titleView.setProgress(max - 1)
        binding.apply {
            pin.clear()
            titleView.setSubTitle(getString(R.string.Confirm_PIN), getSubTitle())
            tipTv.text = getString(R.string.fourth_pin_confirm_hint)
        }
    }

    private fun getSubTitle(): String {
        val change = tipBundle.forChange()
        return when (step) {
            STEP0 -> if (change) "1/5" else throw IllegalArgumentException("")
            STEP1 -> if (change) "2/5" else "1/4"
            STEP2 -> if (change) "3/5" else "2/4"
            STEP3 -> if (change) "4/5" else "3/4"
            STEP4 -> if (change) "5/5" else "4/4"
            else -> throw IllegalArgumentException("")
        }
    }

    private fun validatePin(): Boolean {
        val pin = binding.pin.code()
        if (pin == "123456") {
            toast(R.string.wallet_password_unsafe)
            return false
        }

        val numKind = arrayListOf<Char>()
        pin.forEach {
            if (!numKind.contains(it)) {
                numKind.add(it)
            }
        }
        if (numKind.size <= 2) {
            toast(R.string.wallet_password_unsafe)
            return false
        }

        return true
    }

    private fun createPin() {
        when (step) {
            STEP0 -> verify(binding.pin.code())
            STEP1 -> toStep2(true)
            STEP2 -> toStep3(true)
            STEP3 -> toStep4(true)
            STEP4 -> {
                if (checkEqual()) return

                val pin = binding.pin.code()
                tipBundle.pin = pin
                tipBundle.tipStep = Processing.Creating
                val tipFragment = TipFragment.newInstance(tipBundle)
                activity?.replaceFragment(tipFragment, R.id.container, TipFragment.TAG)
            }
        }
    }

    private fun checkEqual(): Boolean {
        if (lastPassword != binding.pin.code()) {
            toast(R.string.wallet_password_not_equal)
            toStep1()
            return true
        }
        return false
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = getString(R.string.Please_wait_a_bit),
            title = getString(R.string.Verifying)
        )
        dialog.setCancelable(false)
        dialog.show()

        handleMixinResponse(
            invokeNetwork = { walletViewModel.verifyPin(pinCode) },
            successBlock = { response ->
                dialog.dismiss()
                context?.updatePinCheck()
                response.data?.let {
                    val pin = binding.pin.code()
                    val tipBundle = requireArguments().getTipBundle()
                    tipBundle.oldPin = pin
                    toStep1()
                    binding.pin.clear()
                }
            },
            exceptionBlock = {
                if (it is TipNetworkException) {
                    return@handleMixinResponse handleFailure(it.error, dialog)
                } else {
                    dialog.dismiss()
                    binding.pin.clear()
                    return@handleMixinResponse false
                }
            },
            failureBlock = {
                return@handleMixinResponse handleFailure(requireNotNull(it.error), dialog)
            }
        )
    }

    private suspend fun handleFailure(error: ResponseError, dialog: Dialog): Boolean {
        binding.pin.clear()
        if (error.code == ErrorHandler.TOO_MANY_REQUEST) {
            dialog.dismiss()
            toast(R.string.error_pin_check_too_many_request)
            return true
        } else if (error.code == ErrorHandler.PIN_INCORRECT) {
            val errorCount = walletViewModel.errorCount()
            toast(requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount))
            dialog.dismiss()
            return true
        }
        dialog.dismiss()
        return false
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            if (position == 11) {
                binding.pin.delete()
            } else {
                binding.pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            if (position == 11) {
                binding.pin.clear()
            } else {
                binding.pin.append(value)
            }
        }
    }
}

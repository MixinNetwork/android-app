package one.mixin.android.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentOldPasswordBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.withArgs
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.tip.Tip
import one.mixin.android.tip.TipNetworkException
import one.mixin.android.tip.checkAndPublishTipCounterSynced
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipFragment.Companion.ARGS_TIP_BUNDLE
import one.mixin.android.ui.tip.getTipBundle
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import javax.inject.Inject

@AndroidEntryPoint
class OldPasswordFragment : BaseFragment(R.layout.fragment_old_password), PinView.OnPinListener {

    companion object {
        const val TAG = "OldPasswordFragment"

        fun newInstance(tipBundle: TipBundle) = OldPasswordFragment().withArgs {
            putParcelable(ARGS_TIP_BUNDLE, tipBundle)
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private val binding by viewBinding(FragmentOldPasswordBinding::bind)

    @Inject
    lateinit var tip: Tip
    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            titleView.rightAnimator.setOnClickListener { verify(binding.pin.code()) }
            titleView.setSubTitle(getString(R.string.Old_PIN), "1/5")
            disableTitleRight()
            pin.setListener(this@OldPasswordFragment)
            keyboard.setKeyboardKeys(KEYS)
            keyboard.setOnClickKeyboardListener(keyboardListener)
            keyboard.animate().translationY(0f).start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onUpdate(index: Int) {
        if (index == binding.pin.getCount()) {
            binding.apply {
                titleView.rightTv.setTextColor(resources.getColor(R.color.colorBlue, null))
                titleView.rightAnimator.isEnabled = true
            }
        } else {
            disableTitleRight()
        }
    }

    private fun disableTitleRight() {
        binding.apply {
            titleView.rightTv.setTextColor(resources.getColor(R.color.text_gray, null))
            titleView.rightAnimator.isEnabled = false
        }
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = getString(R.string.Please_wait_a_bit),
            title = getString(R.string.Verifying)
        )
        dialog.setCancelable(false)
        dialog.show()

        if (checkAndPublishTipCounterSynced(tip, tipCounterSynced)) {
            dialog.dismiss()
            binding.pin.clear()
            return@launch
        }

        handleMixinResponse(
            invokeNetwork = { walletViewModel.verifyPin(pinCode) },
            successBlock = { response ->
                dialog.dismiss()
                context?.updatePinCheck()
                response.data?.let {
                    val pin = binding.pin.code()
                    val tipBundle = requireArguments().getTipBundle()
                    tipBundle.oldPin = pin
                    navTo(
                        WalletPasswordFragment.newInstance(tipBundle),
                        WalletPasswordFragment.TAG
                    )
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

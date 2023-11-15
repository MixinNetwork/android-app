package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentFullVerifyBottomSheetBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class FullVerifyBottomSheetDialogFragment : MixinBottomSheetDialogFragment(), PinView.OnPinListener {
    companion object {
        const val TAG = "FullVerifyBottomSheetDialogFragment"

        fun newInstance() = FullVerifyBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentFullVerifyBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.apply {
            pin.setListener(this@FullVerifyBottomSheetDialogFragment)
            keyboard.initPinKeys(requireContext())
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
            verify(binding.pin.code())
        }
    }

    private fun showLoading() {
        if (viewDestroyed()) return
        binding.apply {
            verifyFab.visibility = View.VISIBLE
            verifyFab.show()
            verifyCover.visibility = View.VISIBLE
        }
    }

    private fun hideLoading() {
        if (viewDestroyed()) return
        binding.apply {
            verifyFab.hide()
            pin.clear()
            verifyFab.visibility = View.GONE
            verifyCover.visibility = View.GONE
        }
    }

    private fun clearPin() {
        if (viewDestroyed()) return
        binding.pin.clear()
    }

    private fun verify(pinCode: String) =
        lifecycleScope.launch {
            showLoading()

            handleMixinResponse(
                invokeNetwork = {
                    bottomViewModel.verifyPin(pinCode)
                },
                successBlock = {
                    hideLoading()
                    clearPin()
                    context?.updatePinCheck()
                    activity?.supportFragmentManager?.inTransaction {
                        remove(this@FullVerifyBottomSheetDialogFragment)
                    }
                    defaultSharedPreferences.putBoolean(PREF_LOGIN_VERIFY, false)
                    dismiss()
                },
                failureBlock = {
                    return@handleMixinResponse handleFailure(requireNotNull(it.error))
                },
                exceptionBlock = {
                    if (it is TipNetworkException) {
                        return@handleMixinResponse handleFailure(it.error)
                    } else {
                        hideLoading()
                        clearPin()
                        return@handleMixinResponse false
                    }
                },
            )
        }

    private suspend fun handleFailure(error: ResponseError): Boolean {
        clearPin()
        if (error.code == ErrorHandler.TOO_MANY_REQUEST) {
            hideLoading()
            toast(R.string.error_pin_check_too_many_request)
            return true
        } else if (error.code == ErrorHandler.PIN_INCORRECT) {
            val errorCount = bottomViewModel.errorCount()
            hideLoading()
            toast(
                requireContext().resources.getQuantityString(R.plurals.error_pin_incorrect_with_times, errorCount, errorCount),
            )
            return true
        }
        hideLoading()
        return false
    }

    private val keyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(
                position: Int,
                value: String,
            ) {
                context?.tickVibrate()
                if (position == 11) {
                    binding.pin.delete()
                } else {
                    binding.pin.append(value)
                }
            }

            override fun onLongClick(
                position: Int,
                value: String,
            ) {
                context?.clickVibrate()
                if (position == 11) {
                    binding.pin.clear()
                } else {
                    binding.pin.append(value)
                }
            }
        }
}

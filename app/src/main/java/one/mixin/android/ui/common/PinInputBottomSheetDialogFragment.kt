package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_TITLE
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPinInputBottomSheetBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.ui.common.biometric.BiometricLayout
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class PinInputBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinInputBottomSheetDialogFragment"
        private const val ARGS_DISABLE_BIOMETRIC = "args_disable_biometric"
        fun newInstance(title: String? = null, disableBiometric: Boolean = true) = PinInputBottomSheetDialogFragment().withArgs {
            title?.let { putString(ARGS_TITLE, it) }
            putBoolean(ARGS_DISABLE_BIOMETRIC, disableBiometric)
        }
    }

    private val binding by viewBinding(FragmentPinInputBottomSheetBinding::inflate)

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            val titleString = arguments?.getString(ARGS_TITLE)
            if (!titleString.isNullOrBlank()) {
                title.text = titleString
            }
            titleView.rightIv.setOnClickListener { dismiss() }
            val disableBiometric = arguments?.getBoolean(ARGS_DISABLE_BIOMETRIC) ?: false
            if (disableBiometric) {
                biometricLayout.biometricTv.isVisible = false
            } else {
                biometricLayout.biometricTv.setText(R.string.Verify_by_Biometric)
            }
            biometricLayout.pin.setListener(object : PinView.OnPinListener {
                override fun onUpdate(index: Int) {
                    if (index == biometricLayout.pin.getCount()) {
                        onPinComplete?.invoke(biometricLayout.pin.code())
                        dismiss()
                    }
                }
            })
            biometricLayout.measureAllChildren = false
            biometricLayout.callback = biometricLayoutCallback
            keyboard.setKeyboardKeys(KEYS)
            keyboard.setOnClickKeyboardListener(keyboardListener)
        }
    }

    private val biometricLayoutCallback = object : BiometricLayout.Callback {
        override fun onPinComplete(pin: String) {
            onPinComplete?.invoke(pin)
        }

        override fun onShowBiometric() {
            showBiometricPrompt()
        }

        override fun onDismiss() {
            dismiss()
        }
    }

    private var biometricDialog: BiometricDialog? = null
    private fun showBiometricPrompt() {
        biometricDialog = BiometricDialog(
            requireActivity(),
            getBiometricInfo()
        )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    fun getBiometricInfo() = BiometricInfo(
        getString(R.string.Verify_by_Biometric),
        "",
        "",
        getString(R.string.Verify_PIN)
    )

    private val biometricDialogCallback = object : BiometricDialog.Callback {
        override fun onPinComplete(pin: String) {
            onPinComplete?.invoke(pin)
        }

        override fun showPin() {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            binding.biometricLayout.showPin(false)
        }

        override fun showAuthenticationScreen() {
            BiometricUtil.showAuthenticationScreen(this@PinInputBottomSheetDialogFragment.requireActivity())
        }

        override fun onCancel() {
            context?.let {
                binding.biometricLayout.isBiometricTextVisible(BiometricUtil.shouldShowBiometric(it))
            }
        }
    }

    private val keyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            if (position == 11) {
                binding.biometricLayout.pin.delete()
            } else {
                binding.biometricLayout.pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            if (position == 11) {
                binding.biometricLayout.pin.clear()
            } else {
                binding.biometricLayout.pin.append(value)
            }
        }
    }

    fun setOnPinComplete(callback: (String) -> Unit): PinInputBottomSheetDialogFragment {
        onPinComplete = callback
        return this
    }

    private var onPinComplete: ((String) -> Unit)? = null
}

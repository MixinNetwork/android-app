package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_TITLE
import one.mixin.android.databinding.FragmentPinInputBottomSheetBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.biometric.BiometricDialog
import one.mixin.android.ui.common.biometric.BiometricInfo
import one.mixin.android.util.BiometricUtil
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class PinInputBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinInputBottomSheetDialogFragment"
        const val ARGS_BIOMETRIC_INFO = "args_biometric_info"
        const val ARGS_FROM = "args_from"

        fun newInstance(
            title: String? = null,
            biometricInfo: BiometricInfo? = null,
            from: Int = 0
        ) =
            PinInputBottomSheetDialogFragment().withArgs {
                title?.let { putString(ARGS_TITLE, it) }
                biometricInfo?.let { putParcelable(ARGS_BIOMETRIC_INFO, it) }
                putInt(ARGS_FROM, from)
            }
    }

    private val binding by viewBinding(FragmentPinInputBottomSheetBinding::inflate)

    private var biometricDialog: BiometricDialog? = null

    private val biometricInfo: BiometricInfo? by lazy {
        arguments?.getParcelableCompat(ARGS_BIOMETRIC_INFO, BiometricInfo::class.java)
    }

    private val from by lazy {
        arguments?.getInt(ARGS_FROM) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        biometricDialog?.callback = null
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.apply {
            val titleString = arguments?.getString(ARGS_TITLE)
            if (!titleString.isNullOrBlank()) {
                title.text = titleString
            }
            if (biometricInfo != null) {
                biometricTv.isVisible = BiometricUtil.shouldShowBiometric(requireContext())
                biometricTv.setOnClickListener { showBiometricPrompt() }
            }
            icon.isVisible = from != 0
            titleView.rightIv.setOnClickListener { dismiss() }
            pin.setListener(
                object : PinView.OnPinListener {
                    override fun onUpdate(index: Int) {
                        if (index == pin.getCount()) {
                            if (onComplete != null) {
                                onComplete?.invoke(pin.code(), this@PinInputBottomSheetDialogFragment)
                                return
                            }
                            onPinComplete?.invoke(pin.code())
                            dismiss()
                        }
                    }
                },
            )
            keyboard.initPinKeys(requireContext())
            keyboard.setOnClickKeyboardListener(keyboardListener)
        }
    }

    private val keyboardListener =
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

    private fun showBiometricPrompt() {
        val info = this.biometricInfo ?: return
        biometricDialog =
            BiometricDialog(
                requireActivity(),
                info,
            )
        biometricDialog?.callback = biometricDialogCallback
        biometricDialog?.show()
    }

    private val biometricDialogCallback =
        object : BiometricDialog.Callback {
            override fun onPinComplete(pin: String) {
                onPinComplete?.invoke(pin)
                dismiss()
            }

            override fun showPin() {
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            override fun onCancel() {
            }
        }

    fun setOnPinComplete(callback: (String) -> Unit): PinInputBottomSheetDialogFragment {
        onPinComplete = callback
        return this
    }

    private var onPinComplete: ((String) -> Unit)? = null

    fun setOnComplete(callback: (String, PinInputBottomSheetDialogFragment) -> Unit): PinInputBottomSheetDialogFragment {
        onComplete = callback
        return this
    }

    private var onComplete: ((String, PinInputBottomSheetDialogFragment) -> Unit)? = null
}

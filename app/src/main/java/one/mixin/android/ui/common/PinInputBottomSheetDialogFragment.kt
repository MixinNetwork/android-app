package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_TITLE
import one.mixin.android.Constants.KEYS
import one.mixin.android.databinding.FragmentPinInputBottomSheetBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class PinInputBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PinInputBottomSheetDialogFragment"
        fun newInstance(title: String? = null) = PinInputBottomSheetDialogFragment().withArgs {
            title?.let { putString(ARGS_TITLE, it) }
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
            pin.setListener(object : PinView.OnPinListener {
                override fun onUpdate(index: Int) {
                    if (index == pin.getCount()) {
                        onPinComplete?.invoke(pin.code())
                        dismiss()
                    }
                }
            })
            keyboard.setKeyboardKeys(KEYS)
            keyboard.setOnClickKeyboardListener(keyboardListener)
        }
    }

    private val keyboardListener = object : Keyboard.OnClickKeyboardListener {
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

    fun setOnPinComplete(callback: (String) -> Unit): PinInputBottomSheetDialogFragment {
        onPinComplete = callback
        return this
    }

    private var onPinComplete: ((String) -> Unit)? = null
}

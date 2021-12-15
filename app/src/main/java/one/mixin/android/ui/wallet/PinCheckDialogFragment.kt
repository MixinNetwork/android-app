package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentPinCheckBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.realSize
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView

@AndroidEntryPoint
class PinCheckDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "PinCheckDialogFragment"
        const val POS_PIN = 0
        const val POS_PB = 1
        const val POS_TIP = 2

        fun newInstance() = PinCheckDialogFragment()
    }

    private val binding by viewBinding(FragmentPinCheckBinding::inflate)

    private val pinCheckViewModel by viewModels<PinCheckViewModel>()

    private val disposable = CompositeDisposable()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.setContentView(binding.root)

        binding.apply {
            pin.setListener(
                object : PinView.OnPinListener {
                    override fun onUpdate(index: Int) {
                        if (index == pin.getCount()) {
                            verify(pin.code())
                        }
                    }
                }
            )
            gotItTv.setOnClickListener { activity?.finish() }
            keyboard.apply {
                setKeyboardKeys(KEYS)
                setOnClickKeyboardListener(mKeyboardListener)
                animate().translationY(0f).start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        binding.apply {
            pinVa.displayedChild = POS_PB
            handleMixinResponse(
                invokeNetwork = { pinCheckViewModel.verifyPin(pinCode) },
                successBlock = {
                    pin.clear()
                    pinVa.displayedChild = POS_PIN
                    context?.updatePinCheck()
                    dismiss()
                },
                exceptionBlock = {
                    pin.clear()
                    pinVa.displayedChild = POS_PIN
                    return@handleMixinResponse false
                },
                failureBlock = { response ->
                    pin.clear()
                    if (response.errorCode == ErrorHandler.PIN_INCORRECT) {
                        val errorCount = pinCheckViewModel.errorCount()
                        pinVa.displayedChild = POS_PIN
                        pin.error(getString(R.string.error_pin_incorrect_with_times, ErrorHandler.PIN_INCORRECT, errorCount))
                    } else if (response.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                        pinVa.displayedChild = POS_TIP
                        tipVa.showNext()
                        val transY = root.height / 2 - topLl.translationY * 2
                        topLl.animate()?.translationY(transY)?.start()
                        keyboard.animate()?.translationY(keyboard.height.toFloat())?.start()
                    } else {
                        pinVa.displayedChild = POS_PIN
                        pin.error(requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription))
                    }
                    return@handleMixinResponse false
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val displaySize = requireContext().realSize()
        dialog?.window?.setLayout(displaySize.x, MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_transparent_dialog)
        dialog?.window?.setGravity(Gravity.BOTTOM)

        // reset top layout position
        binding.apply {
            root.post {
                val keyboardHeight = keyboard.height
                val h = root.height
                val topHeight = topLl.height
                val margin = (h - keyboardHeight - topHeight) / 2
                if (margin > 0) {
                    topLl.translationY = margin.toFloat()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK) {
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable.dispose()
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: IllegalStateException) {
        }
    }

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener = object : Keyboard.OnClickKeyboardListener {
        override fun onKeyClick(position: Int, value: String) {
            context?.tickVibrate()
            binding.apply {
                if (position == 11) {
                    pin.delete()
                } else {
                    pin.append(value)
                }
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.clickVibrate()
            binding.apply {
                if (position == 11) {
                    pin.clear()
                } else {
                    pin.append(value)
                }
            }
        }
    }
}

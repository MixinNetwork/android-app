package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_pin_check.view.*
import kotlinx.coroutines.launch
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
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

    private lateinit var contentView: View

    private val pinCheckViewModel by viewModels<PinCheckViewModel>()

    private val disposable = CompositeDisposable()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_pin_check, null)
        dialog.setContentView(contentView)

        contentView.pin.setListener(
            object : PinView.OnPinListener {
                override fun onUpdate(index: Int) {
                    if (index == contentView.pin.getCount()) {
                        verify(contentView.pin.code())
                    }
                }
            }
        )
        contentView.got_it_tv.setOnClickListener { activity?.finish() }
        contentView.keyboard.setKeyboardKeys(KEYS)
        contentView.keyboard.setOnClickKeyboardListener(mKeyboardListener)
        contentView.keyboard.animate().translationY(0f).start()
    }

    private fun verify(pinCode: String) = lifecycleScope.launch {
        contentView.pin_va?.displayedChild = POS_PB
        handleMixinResponse(
            invokeNetwork = { pinCheckViewModel.verifyPin(pinCode) },
            successBlock = {
                contentView.pin?.clear()
                contentView.pin_va?.displayedChild = POS_PIN
                context?.updatePinCheck()
                dismiss()
            },
            exceptionBlock = {
                contentView.pin?.clear()
                contentView.pin_va?.displayedChild = POS_PIN
                return@handleMixinResponse false
            },
            failureBlock = { response ->
                contentView.pin?.clear()
                if (response.errorCode == ErrorHandler.PIN_INCORRECT) {
                    val errorCount = pinCheckViewModel.errorCount()
                    contentView.pin_va?.displayedChild = POS_PIN
                    contentView.pin?.error(getString(R.string.error_pin_incorrect_with_times, ErrorHandler.PIN_INCORRECT, errorCount))
                } else if (response.errorCode == ErrorHandler.TOO_MANY_REQUEST) {
                    contentView.pin_va?.displayedChild = POS_TIP
                    contentView.tip_va?.showNext()
                    val transY = contentView.height / 2 - contentView.top_ll.translationY * 2
                    contentView.top_ll?.animate()?.translationY(transY)?.start()
                    contentView.keyboard?.animate()?.translationY(contentView.keyboard.height.toFloat())?.start()
                } else {
                    contentView.pin_va?.displayedChild = POS_PIN
                    contentView.pin?.error(requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription))
                }
                return@handleMixinResponse false
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val displaySize = requireContext().realSize()
        dialog?.window?.setLayout(displaySize.x, MATCH_PARENT)
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_transparent_dialog)
        dialog?.window?.setGravity(Gravity.BOTTOM)

        // reset top layout position
        contentView.post {
            val keyboardHeight = contentView.keyboard.height
            val h = contentView.height
            val topHeight = contentView.top_ll.height
            val margin = (h - keyboardHeight - topHeight) / 2
            if (margin > 0) {
                contentView.top_ll.translationY = margin.toFloat()
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
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                contentView.pin.delete()
            } else {
                contentView.pin.append(value)
            }
        }

        override fun onLongClick(position: Int, value: String) {
            context?.vibrate(longArrayOf(0, 30))
            if (position == 11) {
                contentView.pin.clear()
            } else {
                contentView.pin.append(value)
            }
        }
    }
}

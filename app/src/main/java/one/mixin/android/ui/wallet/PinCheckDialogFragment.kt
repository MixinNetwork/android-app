package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.ResponseError
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentPinCheckBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.realSize
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.tip.exception.TipNetworkException
import one.mixin.android.tip.getTipExceptionMsg
import one.mixin.android.tip.isTipNodeException
import one.mixin.android.ui.common.TaskDialogActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.isExpandedSheetScreen
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

    private var dialogCallback: ((Boolean) -> Unit)? = null

    fun setDialogCallback(dialogCallback: ((Boolean) -> Unit)?) {
        this.dialogCallback = dialogCallback
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        isCancelable = false
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialogCallback?.invoke(true)
        dialog.setContentView(binding.root)
        binding.apply {
            pin.setListener(
                object : PinView.OnPinListener {
                    override fun onUpdate(index: Int) {
                        if (index == pin.getCount()) {
                            verify(pin.code())
                        }
                    }
                },
            )
            gotItTv.setOnClickListener { if (activity is TaskDialogActivity) activity?.finishAffinity() else activity?.finish() }
            keyboard.apply {
                initPinKeys(requireContext())
                setOnClickKeyboardListener(mKeyboardListener)
                animate().translationY(0f).start()
            }
            if (requireContext().isExpandedSheetScreen()) {
                // One compact card: the PIN box spans the card and the keyboard follows it directly.
                root.background =
                    GradientDrawable().apply {
                        setColor(requireContext().colorFromAttribute(R.attr.bg_window))
                        cornerRadius = 24.dp.toFloat()
                    }
                root.clipToOutline = true
                topLl.updateLayoutParams<RelativeLayout.LayoutParams> {
                    marginStart = 0
                    marginEnd = 0
                }
                keyboard.updateLayoutParams<RelativeLayout.LayoutParams> {
                    removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(RelativeLayout.BELOW, R.id.top_ll)
                }
                keyboard.animate().cancel()
                keyboard.translationY = 0f
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogCallback?.invoke(false)
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun verify(pinCode: String) =
        lifecycleScope.launch {
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
                        if (it is TipNetworkException) {
                            handleFailure(it.error)
                        } else if (it.isTipNodeException()) {
                            pinVa.displayedChild = POS_PIN
                            pin.error(it.getTipExceptionMsg(requireContext(), null))
                        } else {
                            pin.clear()
                            pinVa.displayedChild = POS_PIN
                        }
                        return@handleMixinResponse false
                    },
                    failureBlock = { response ->
                        handleFailure(requireNotNull(response.error))
                        return@handleMixinResponse false
                    },
                )
            }
        }

    private fun handleFailure(error: ResponseError) =
        lifecycleScope.launch {
            binding.apply {
                pin.clear()
                when (error.code) {
                    ErrorHandler.PIN_INCORRECT -> {
                        val errorCount = pinCheckViewModel.errorCount()
                        pinVa.displayedChild = POS_PIN
                        pin.error(
                            requireContext().resources.getQuantityString(
                                R.plurals.error_pin_incorrect_with_times,
                                errorCount,
                                errorCount,
                            ),
                        )
                    }
                    ErrorHandler.TOO_MANY_REQUEST -> {
                        pinVa.displayedChild = POS_TIP
                        tipVa.showNext()
                        if (requireContext().isExpandedSheetScreen()) {
                            keyboard.isVisible = false
                            return@launch
                        }
                        val transY = root.height / 2 - topLl.translationY * 2
                        topLl.animate()?.translationY(transY)?.start()
                        keyboard.animate()?.translationY(keyboard.height.toFloat())?.start()
                    }
                    else -> {
                        pinVa.displayedChild = POS_PIN
                        pin.error(requireContext().getMixinErrorStringByCode(error.code, error.description))
                    }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        val displaySize = requireContext().realSize()
        if (requireContext().isExpandedSheetScreen()) {
            dialog?.window?.setLayout(minOf(displaySize.x, 420.dp), WRAP_CONTENT)
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setGravity(Gravity.CENTER)
            return
        } else {
            dialog?.window?.setLayout(displaySize.x, MATCH_PARENT)
            dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_transparent_dialog)
            dialog?.window?.setGravity(Gravity.BOTTOM)
        }

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
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // block back press
                }
            },
        )
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

    private val mKeyboardListener: Keyboard.OnClickKeyboardListener =
        object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(
                position: Int,
                value: String,
            ) {
                context?.tickVibrate()
                binding.apply {
                    if (position == 11) {
                        pin.delete()
                    } else {
                        pin.append(value)
                    }
                }
            }

            override fun onLongClick(
                position: Int,
                value: String,
            ) {
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

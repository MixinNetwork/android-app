package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWarningBottomSheetBinding
import one.mixin.android.extension.textColor
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class WarningBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WarningBottomSheetDialogFragment"

        const val ARGS_WARNING = "args_warning"
        const val ARGS_SECONDS = "args_seconds"

        fun newInstance(
            warning: String,
            seconds: Int = 3,
        ) = WarningBottomSheetDialogFragment().withArgs {
            putString(ARGS_WARNING, warning)
            putInt(ARGS_SECONDS, seconds)
        }
    }

    var callback: Callback? = null

    private val binding by viewBinding(FragmentWarningBottomSheetBinding::inflate)

    private val warning: String by lazy {
        requireArguments().getString(ARGS_WARNING, "")
    }
    private val seconds: Int by lazy {
        requireArguments().getInt(ARGS_SECONDS, 3)
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }

        binding.apply {
            continueTv.setOnClickListener {
                callback?.onContinue()
                dismiss()
            }
            warningTv.text = warning
        }

        startCountDown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private var countDownTimer: CountDownTimer? = null

    private fun startCountDown() {
        countDownTimer?.cancel()
        binding.apply {
            continueTv.isEnabled = false
            continueTv.textColor = requireContext().getColor(R.color.wallet_text_gray)
            countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {

                override fun onTick(l: Long) {
                    continueTv.text =
                        requireContext().getString(R.string.got_it_count_down, l / 1000)
                }

                override fun onFinish() {
                    continueTv.text = getString(R.string.got_it)
                    continueTv.isEnabled = true
                    continueTv.textColor = requireContext().getColor(R.color.white)
                }
            }
            countDownTimer?.start()
        }
    }

    interface Callback {
        fun onContinue()
    }
}

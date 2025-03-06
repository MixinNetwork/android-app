package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.CountDownTimer
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLandingDeleteAccountBinding
import one.mixin.android.extension.createAtToLong
import one.mixin.android.extension.localDateString
import one.mixin.android.extension.textColor
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class LandingDeleteAccountFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "LandingDeleteAccountFragment"

        private const val ARGS_REQUEST_TIME = "ARGS_REQUEST_TIME"
        private const val ARGS_EFFECTIVE_TIME = "ARGS_EFFECTIVE_TIME"

        fun newInstance(requestTime: String?, effectiveTime: String?) =
            LandingDeleteAccountFragment().withArgs {
                requestTime?.let {
                    putLong(ARGS_REQUEST_TIME, it.createAtToLong())
                }
                effectiveTime?.let {
                    putLong(ARGS_EFFECTIVE_TIME, it.createAtToLong())
                }
            }
    }

    private val binding by viewBinding(FragmentLandingDeleteAccountBinding::inflate)

    private val requestTime: Long by lazy {
        requireArguments().getLong(ARGS_REQUEST_TIME, System.currentTimeMillis())
    }

    private val effectiveTime: Long by lazy {
        requireArguments().getLong(ARGS_EFFECTIVE_TIME, System.currentTimeMillis())
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }
        binding.apply {
            content.text =
                getString(
                    R.string.landing_delete_content,
                    localDateString(requestTime),
                    localDateString(effectiveTime),
                )
            continueTv.setOnClickListener {
                dismiss()
                continueCallback?.invoke()
            }
            cancelTv.setOnClickListener {
                dismiss()
            }
        }
        startCountDown()
    }

    fun setContinueCallback(callback: () -> Unit): LandingDeleteAccountFragment {
        continueCallback = callback
        return this
    }

    private var continueCallback: (() -> Unit)? = null

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
            countDownTimer =
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(l: Long) {
                        continueTv.text =
                            requireContext().getString(R.string.got_it_count_down, l / 1000)
                    }

                    override fun onFinish() {
                        continueTv.text = getString(R.string.Got_it)
                        continueTv.isEnabled = true
                        continueTv.textColor = requireContext().getColor(R.color.white)
                    }
                }
            countDownTimer?.start()
        }
    }
}

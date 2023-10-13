package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDisappearingIntervalBottomSheetBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DisappearingIntervalBottomFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "DisappearingIntervalBottomFragment"
        private const val TIME_INTERVAL = "time_interval"

        fun newInstance(timeInterval: Long?) = DisappearingIntervalBottomFragment().withArgs {
            if (timeInterval != null) {
                putLong(TIME_INTERVAL, timeInterval)
            }
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val binding by viewBinding(FragmentDisappearingIntervalBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.apply {
            intervalPicker.initTimeInterval(arguments?.getLong(TIME_INTERVAL, 0L))
            leftIb.setOnClickListener {
                dismiss()
            }
            rightTv.setOnClickListener {
                onSetCallback?.invoke(intervalPicker.getTimeInterval())
                dismiss()
            }
        }
        (dialog as BottomSheet).setCustomView(contentView)
    }

    private var onSetCallback: ((Long) -> Unit)? = null

    fun onSetCallback(onSetCallback: ((Long) -> Unit)?) {
        this.onSetCallback = onSetCallback
    }
}

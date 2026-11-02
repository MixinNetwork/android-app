package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDesktopHelpBinding
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class DesktopHelpBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DesktopHelpBottomSheetDialogFragment"

        fun newInstance() = DesktopHelpBottomSheetDialogFragment()
    }

    override val centerOnExpandedScreen = true

    private val binding by viewBinding(FragmentDesktopHelpBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)

        binding.linkTv.setOnClickListener {
            requireContext().openUrl("https://${getString(R.string.desktop_help_link)}")
        }
        binding.gotItBtn.setOnClickListener { dismiss() }
    }
}

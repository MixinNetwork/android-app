package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.databinding.FragmentAssetMigrationBottomSheetBinding
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class AssetMigrationBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AssetMigrationBottomSheetDialogFragment"

        fun newInstance() = AssetMigrationBottomSheetDialogFragment().apply {
        }
    }

    private val binding by viewBinding(FragmentAssetMigrationBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.title.centerTitle()
        binding.title.rightIv.setOnClickListener { dismiss() }
        binding.startBn.setOnClickListener {
            callback?.invoke()
            dismiss()
        }
    }

    var callback: (() -> Unit)? = null
}

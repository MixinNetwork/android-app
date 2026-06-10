package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMemoBottomSheetBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.FormatMemo
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class MemoBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "ShareMessageBottomSheetDialogFragment"
        private const val MEMO = "memo"

        fun newInstance(memo: FormatMemo) =
            MemoBottomSheetDialogFragment().withArgs {
                putParcelable(MEMO, memo)
            }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val memo by lazy {
        arguments?.getParcelableCompat(MEMO, FormatMemo::class.java)!!
    }

    private val binding by viewBinding(FragmentMemoBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "StringFormatInvalid")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.close.setOnClickListener {
            dismiss()
        }
        binding.utfContent.text = memo.utf
        binding.hexContent.text = memo.hex
        if (memo.utf == null) {
            binding.utf.isVisible = false
            binding.utfContent.isVisible = false
            binding.utfCopy.isVisible = false
            (binding.hex.layoutParams as ConstraintLayout.LayoutParams).apply {
                topToBottom = R.id.title
            }
        }
        binding.utfCopy.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(
                ClipData.newPlainText(
                    null,
                    memo.utf,
                ),
            )
            toast(R.string.copied_to_clipboard)
        }

        binding.hexCopy.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(
                ClipData.newPlainText(
                    null,
                    memo.hex,
                ),
            )
            toast(R.string.copied_to_clipboard)
        }
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }
}

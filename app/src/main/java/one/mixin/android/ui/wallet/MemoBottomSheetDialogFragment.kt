package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import androidx.fragment.app.viewModels
import com.bumptech.glide.manager.SupportRequestManagerFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMemoBottomSheetBinding
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.hexString
import one.mixin.android.extension.hexToString
import one.mixin.android.extension.isValidHex
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class MemoBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ShareMessageBottomSheetDialogFragment"
        private const val MEMO = "memo"
        fun newInstance(memo: String) = MemoBottomSheetDialogFragment().withArgs {
            putString(MEMO, memo)
        }
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    private val viewModel by viewModels<BottomSheetViewModel>()

    private val memo by lazy {
        arguments?.getString(MEMO)?.let {
            if (it.isValidHex()) {
                it.hexToString()
            } else {
                it
            }
        }
    }

    private val memoHex by lazy {
        memo?.hexString()
    }

    private val binding by viewBinding(FragmentMemoBottomSheetBinding::inflate)

    @SuppressLint("RestrictedApi", "StringFormatInvalid")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.close.setOnClickListener {
            dismiss()
        }
        binding.utfContent.text = memo
        binding.hexContent.text = memoHex
        binding.utfCopy.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(
                ClipData.newPlainText(
                    null,
                    memo,
                ),
            )
            toast(R.string.copied_to_clipboard)
        }

        binding.hexCopy.setOnClickListener {
            context?.getClipboardManager()?.setPrimaryClip(
                ClipData.newPlainText(
                    null,
                    memoHex,
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
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }
}

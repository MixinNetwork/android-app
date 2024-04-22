package one.mixin.android.web3

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.api.response.Web3Token
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.AppListBottomSheetDialogFragment
import one.mixin.android.vo.App

class TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val ARGS_TOKENS = "args_tokens"
        const val TAG = "TokenListBottomSheetDialogFragment"

        fun newInstance(tokens: ArrayList<Web3Token>) =
            TokenListBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_TOKENS, tokens)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val tokens by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_TOKENS, Web3Token::class.java)
    }

    private val adapter by lazy {
        TokenAdapter()
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            assetRv.adapter = adapter
            adapter.tokens = tokens!!
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
        }
    }

    fun setOnClickListener(onClickListener: (Web3Token) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }
}
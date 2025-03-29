package one.mixin.android.web3.send

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

class TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_TOKENS = "args_tokens"
        const val ARGS_ADDRESS = "args_address"
        const val TAG = "TokenListBottomSheetDialogFragment"

        fun newInstance(tokens: ArrayList<TokenItem>, address: String = "") =
            TokenListBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_TOKENS, tokens)
                putString(ARGS_ADDRESS, address)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val tokens by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_TOKENS, TokenItem::class.java)
    }
    
    private val address by lazy {
        requireArguments().getString(ARGS_ADDRESS, "")
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
            if (tokens.isNullOrEmpty()) {
                rvVa.displayedChild = 2
            } else {
                rvVa.displayedChild = 0
            }
            depositTv.setOnClickListener {
                dismiss()
            }
            searchEt.listener =
                object : SearchView.OnSearchViewListener {
                    override fun afterTextChanged(s: Editable?) {
                        filter(s.toString())
                    }

                    override fun onSearch() {}
                }
        }
    }

    private fun filter(s: String) {
        if (s.isBlank()) {
            adapter.tokens = tokens!!
            if (tokens.isNullOrEmpty()) {
                binding.rvVa.displayedChild = 2
            } else {
                binding.rvVa.displayedChild = 0
            }
            return
        }
        val assetList =
            tokens?.filter {
                it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
            }
        adapter.tokens = assetList?.let { ArrayList(it) } ?: arrayListOf()
        if (adapter.itemCount == 0) {
            binding.rvVa.displayedChild = 1
        } else {
            binding.rvVa.displayedChild = 0
        }
    }

    fun setOnClickListener(onClickListener: (TokenItem) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }
}

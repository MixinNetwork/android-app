package one.mixin.android.web3

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

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
            if (tokens.isNullOrEmpty()) {
                rvVa.displayedChild = 2
            } else {
                rvVa.displayedChild = 0
            }
            depositTv.setText(R.string.Receive)
            depositTv.setOnClickListener {
                navTo(Wbe3DepositFragment(), Wbe3DepositFragment.TAG)
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

    fun setOnClickListener(onClickListener: (Web3Token) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }
}
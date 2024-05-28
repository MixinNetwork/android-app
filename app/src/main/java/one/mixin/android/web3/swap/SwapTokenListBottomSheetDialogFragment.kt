package one.mixin.android.web3.swap

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class SwapTokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_TOKENS = "args_tokens"
        const val TAG = "SwapTokenListBottomSheetDialogFragment"

        fun newInstance(tokens: ArrayList<SwapToken>) =
            SwapTokenListBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_TOKENS, tokens)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)
    private val swapViewModel by viewModels<SwapViewModel>()

    private val tokens by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_TOKENS, SwapToken::class.java)
    }

    private val adapter by lazy {
        SwapTokenAdapter()
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
                navTo(Web3AddressFragment(), Web3AddressFragment.TAG)
                dismiss()
            }
            searchEt.listener =
                object : SearchView.OnSearchViewListener {
                    override fun afterTextChanged(s: Editable?) {
                        lifecycleScope.launch {
                            filter(s.toString())
                        }
                    }

                    override fun onSearch() {}
                }
        }
    }

    private suspend fun filter(s: String) {
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
        adapter.tokens = if (assetList.isNullOrEmpty()) {
            handleMixinResponse(
                invokeNetwork = { swapViewModel.getSwapToken(s) },
                successBlock = { resp ->
                    return@handleMixinResponse resp.data
                }
            )?.let { arrayListOf(it) } ?: arrayListOf()
        } else {
            ArrayList(assetList)
        }
        if (adapter.itemCount == 0) {
            binding.rvVa.displayedChild = 1
        } else {
            binding.rvVa.displayedChild = 0
        }
    }

    fun setOnClickListener(onClickListener: (SwapToken,Boolean) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }
}

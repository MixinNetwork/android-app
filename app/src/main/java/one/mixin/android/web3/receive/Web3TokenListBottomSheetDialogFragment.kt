package one.mixin.android.web3.receive

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
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.SearchView

@AndroidEntryPoint
class Web3TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "Web3TokenListBottomSheetDialogFragment"

        fun newInstance() = Web3TokenListBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private var tokens = ArrayList<Web3TokenItem>()
    
    private val adapter by lazy {
        Web3TokenAdapter()
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
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            rvVa.displayedChild = 0
            depositTv.setText(R.string.Receive)
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
        
        bottomViewModel.web3TokenItems().observe(this) { items ->
            tokens = ArrayList(items)
            adapter.tokens = tokens
            if (tokens.isEmpty()) {
                binding.rvVa.displayedChild = 2
            } else {
                binding.rvVa.displayedChild = 0
            }
        }
    }

    private fun filter(s: String) {
        if (s.isBlank()) {
            adapter.tokens = tokens
            if (tokens.isNullOrEmpty()) {
                binding.rvVa.displayedChild = 2
            } else {
                binding.rvVa.displayedChild = 0
            }
            return
        }
        val assetList =
            tokens.filter {
                it.name.containsIgnoreCase(s) || it.symbol.containsIgnoreCase(s)
            }
        adapter.tokens = ArrayList(assetList)
        if (adapter.itemCount == 0) {
            binding.rvVa.displayedChild = 1
        } else {
            binding.rvVa.displayedChild = 0
        }
    }

    fun setOnClickListener(onClickListener: (Web3TokenItem) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }
}

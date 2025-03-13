package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectListBottomSheetBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.adapter.SelectableWeb3TokenAdapter
import one.mixin.android.ui.wallet.adapter.SelectedWeb3TokenAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchWeb3TokenItemCallback
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class MultiSelectWeb3TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MultiSelectWeb3TokenListBottomSheetDialogFragment"
        const val POS_RV = 0
        const val POS_EMPTY = 1
        const val POS_EMPTY_TOKEN = 2
        const val LIMIT = 10

        fun newInstance() = MultiSelectWeb3TokenListBottomSheetDialogFragment()
    }

    private val binding by viewBinding(FragmentSelectListBottomSheetBinding::inflate)

    private val walletViewModel by viewModels<WalletViewModel>()

    private val selectedTokenItems = mutableListOf<Web3TokenItem>()
    private val adapter by lazy { SelectableWeb3TokenAdapter(selectedTokenItems) }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<Web3TokenItem>()

    private val groupAdapter: SelectedWeb3TokenAdapter by lazy {
        SelectedWeb3TokenAdapter { tokenItem ->
            selectedTokenItems.remove(tokenItem)
            adapter.notifyItemChanged(adapter.currentList.indexOf(tokenItem))
            groupAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        dataProvider?.let { provider ->
            selectedTokenItems.clear()
            selectedTokenItems.addAll(provider.getCurrentTokens())
        }
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            rv.adapter = adapter
            selectRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            selectRv.adapter = groupAdapter
            adapter.callback =
                object : WalletSearchWeb3TokenItemCallback {
                    override fun onTokenItemClick(tokenItem: Web3TokenItem) {
                        binding.searchEt.hideKeyboard()
                        if (selectedTokenItems.contains(tokenItem)) {
                            selectedTokenItems.remove(tokenItem)
                        } else {
                            selectedTokenItems.add(tokenItem)
                        }
                        adapter.notifyItemChanged(adapter.currentList.indexOf(tokenItem))
                        groupAdapter.checkedTokenItems = selectedTokenItems
                        groupAdapter.notifyDataSetChanged()
                        selectRv.scrollToPosition(selectedTokenItems.size - 1)
                    }
                }
            depositTitle.setText(R.string.No_asset)
            depositTv.isVisible = false
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            resetButton.setOnClickListener {
                selectedTokenItems.clear()
                onMultiSelectTokenListener?.onTokenSelect(null)
                dismiss()
            }
            applyButton.setOnClickListener {
                onMultiSelectTokenListener?.onTokenSelect(selectedTokenItems.toList())
                dismiss()
            }
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = POS_RV
                                adapter.submitList(defaultAssets)
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {},
                    )
        }

        bottomViewModel.web3TokenItems()
            .observe(this) { tokens ->
                defaultAssets = tokens
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.submitList(defaultAssets)
                }
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY
                }
            }
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = POS_RV
                binding.pb.isVisible = true

                val localAssets = defaultAssets.filter {
                    it.name.contains(query, true) || it.symbol.contains(query, true)
                }
                adapter.submitList(localAssets) {
                    binding.rv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_TOKEN
                }
            }
    }

    fun setOnMultiSelectTokenListener(onMultiSelectTokenListener: OnMultiSelectTokenListener): MultiSelectWeb3TokenListBottomSheetDialogFragment {
        this.onMultiSelectTokenListener = onMultiSelectTokenListener
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.searchEt.et.setText(null)
        currentQuery = ""
        currentSearch?.cancel()
        onMultiSelectTokenListener?.onDismiss()
    }

    private var onMultiSelectTokenListener: OnMultiSelectTokenListener? = null

    interface OnMultiSelectTokenListener {
        fun onTokenSelect(tokenItems: List<Web3TokenItem>?)
        fun onDismiss()
    }

    interface DataProvider {
        fun getCurrentTokens(): List<Web3TokenItem>
    }

    private var dataProvider: DataProvider? = null
    fun setDateProvider(dataProvider: DataProvider): MultiSelectWeb3TokenListBottomSheetDialogFragment {
        this.dataProvider = dataProvider
        return this
    }
}

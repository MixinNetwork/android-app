package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.adapter.SelectableCoinAdapter
import one.mixin.android.ui.wallet.adapter.SelectedCoinAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCoinItemCallback
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class MultiSelectCoinListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MultiSelectCoinListBottomSheetDialogFragment"
        const val POS_RV = 0
        const val POS_EMPTY = 1
        const val POS_EMPTY_TOKEN = 2
        const val LIMIT = 10

        private const val ARGS_SINGLE = "args_single"

        fun newInstance(single: Boolean = false) = MultiSelectCoinListBottomSheetDialogFragment().withArgs {
            putBoolean(ARGS_SINGLE, single)
        }
    }

    private val binding by viewBinding(FragmentSelectListBottomSheetBinding::inflate)

    private val selectedCoinItems = mutableListOf<CoinItem>()

    private val adapter by lazy { SelectableCoinAdapter(selectedCoinItems, isSingle) }

    private val isSingle by lazy {
        requireArguments().getBoolean(ARGS_SINGLE, false)
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultCoins = emptyList<CoinItem>()

    private val groupAdapter: SelectedCoinAdapter by lazy {
        SelectedCoinAdapter { coinItem ->
            selectedCoinItems.remove(coinItem)
            adapter.notifyItemChanged(adapter.currentList.indexOf(coinItem))
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
            selectedCoinItems.clear()
            selectedCoinItems.addAll(provider.getCurrentCoins())
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
                object : WalletSearchCoinItemCallback {
                    override fun onCoinItemClick(coinItem: CoinItem) {
                        binding.searchEt.hideKeyboard()
                        if (isSingle){
                            onMultiSelectCoinListener?.onCoinClick(coinItem)
                            dismiss()
                        } else {
                            if (selectedCoinItems.contains(coinItem)) {
                                selectedCoinItems.remove(coinItem)
                            } else {
                                selectedCoinItems.add(coinItem)
                            }
                            adapter.notifyItemChanged(adapter.currentList.indexOf(coinItem))
                            groupAdapter.checkedCoinItems = selectedCoinItems
                            groupAdapter.notifyDataSetChanged()
                            selectRv.scrollToPosition(selectedCoinItems.size - 1)
                        }
                    }
                }
            depositTitle.setText(R.string.No_asset)
            depositTv.isVisible = false
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            resetButton.setOnClickListener {
                selectedCoinItems.clear()
                onMultiSelectCoinListener?.onCoinSelect(null)
                dismiss()
            }
            applyButton.setOnClickListener {
                onMultiSelectCoinListener?.onCoinSelect(selectedCoinItems.toList())
                dismiss()
            }
            bottomRl.isVisible = !isSingle
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = POS_RV
                                adapter.submitList(defaultCoins)
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

        bottomViewModel.coinItems()
            .observe(this) {
                defaultCoins = it
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.submitList(defaultCoins)
                }
                if (defaultCoins.isEmpty()) {
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

                val localCoins = defaultCoins.filter {
                    it.name.contains(query, true) || it.symbol.contains(query, true)
                }
                adapter.submitList(localCoins) {
                    binding.rv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localCoins.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_TOKEN
                }
            }
    }

    fun setOnMultiSelectCoinListener(onMultiSelectCoinListener: OnMultiSelectCoinListener): MultiSelectCoinListBottomSheetDialogFragment {
        this.onMultiSelectCoinListener = onMultiSelectCoinListener
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.searchEt.et.text = null
        currentQuery = ""
        currentSearch?.cancel()
        onMultiSelectCoinListener?.onDismiss()
    }

    private var onMultiSelectCoinListener: OnMultiSelectCoinListener? = null

    interface OnMultiSelectCoinListener {
        fun onCoinClick(coinItem: CoinItem)
        fun onCoinSelect(coinItems: List<CoinItem>?)
        fun onDismiss()
    }

    interface DataProvider {
        fun getCurrentCoins(): List<CoinItem>
    }

    private var dataProvider: DataProvider? = null
    fun setDateProvider(dataProvider:DataProvider): MultiSelectCoinListBottomSheetDialogFragment {
        this.dataProvider = dataProvider
        return this
    }
}
package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAllTransactionsBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.withArgs
import one.mixin.android.job.RefreshWeb3TransactionJob
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.adapter.Web3TransactionPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.details.Web3TransactionFragment
import timber.log.Timber

@AndroidEntryPoint
class AllWeb3TransactionsFragment : BaseTransactionsFragment<PagedList<Web3TransactionItem>>(R.layout.fragment_all_transactions) {
    companion object {
        const val TAG = "AllTransactionsFragment"
        const val ARGS_TOKEN = "args_token"

        fun newInstance(tokenItem: Web3TokenItem? = null): AllWeb3TransactionsFragment {
            return AllWeb3TransactionsFragment().withArgs {
                putParcelable(ARGS_TOKEN, tokenItem)
            }
        }
    }

    private val binding by viewBinding(FragmentAllTransactionsBinding::bind)

    private val adapter = Web3TransactionPagedAdapter().apply {
        setOnItemClickListener(object : Web3TransactionPagedAdapter.OnItemClickListener {
            override fun onItemClick(transaction: Web3TransactionItem) {
                lifecycleScope.launch {
                    val token =
                        web3ViewModel.web3TokenItemById(transaction.assetId) ?: return@launch
                    navTo(
                        Web3TransactionFragment.newInstance(transaction, transaction.chainId, token),
                        Web3TransactionFragment.TAG
                    )
                }
            }
        })
    }
    private val tokenItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java)
    }

    private val filterParams by lazy {
        Web3FilterParams(tokenItems = tokenItem?.let { listOf(it) })
    }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var refreshJob: Job? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightAnimator.setOnClickListener {
                    menuAdapter.checkPosition = when (filterParams.order) {
                        SortOrder.Recent -> 0
                        SortOrder.Oldest -> 1
                        SortOrder.Value -> 2
                        else -> 3
                    }
                    sortMenu.show()
                }
            }
            transactionsRv.itemAnimator = null
            transactionsRv.adapter = adapter
            val layoutManager = LinearLayoutManager(requireContext())
            transactionsRv.layoutManager = layoutManager
            transactionsRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
            adapter.registerAdapterDataObserver(
                object : RecyclerView.AdapterDataObserver() {
                    override fun onItemRangeInserted(
                        positionStart: Int,
                        itemCount: Int,
                    ) {
                        val firstPos = layoutManager.findFirstVisibleItemPosition()
                        if (firstPos == 0) {
                            layoutManager.scrollToPosition(0)
                        }
                    }
                },
            )
        }
        dataObserver =
            Observer { pagedList ->
                if (pagedList.isNotEmpty()) {
                    showEmpty(false)
                } else {
                    showEmpty(true)
                }
                adapter.submitList(pagedList)
            }
        bindLiveData()
        binding.apply {
            filterType.setOnClickListener {
                filterType.open()
                typeAdapter.checkPosition = when (filterParams.tokenFilterType) {
                    Web3TokenFilterType.ALL -> 0
                    Web3TokenFilterType.SEND -> 1
                    else -> 2
                }
                typeMenu.show()
            }
            filterAsset.setOnClickListener {
                selectAsset()
            }
            filterUser.isVisible = false
            filterTime.setOnClickListener {
                datePicker()
            }
            loadFilter()
        }
        jobManager.addJobInBackground(RefreshWeb3TransactionJob())
    }

    override fun onResume() {
        super.onResume()
        startRefreshData()
    }

    override fun onPause() {
        super.onPause()
        cancelRefreshData()
    }

    private fun startRefreshData() {
        cancelRefreshData()
        refreshJob = lifecycleScope.launch {
            refreshTransactionData()
        }
    }

    private fun cancelRefreshData() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private suspend fun refreshTransactionData() {
        try {
            while (true) {
                val pendingRawTransaction = web3ViewModel.getPendingTransactions()
                if (pendingRawTransaction.isEmpty()) {
                    delay(10_000)
                } else {
                    pendingRawTransaction.forEach { transition ->
                        val r = web3ViewModel.transaction(transition.hash, transition.chainId)
                        if (r.isSuccess) {
                            // Todo update transaction
                            web3ViewModel.deletePending(transition.hash)
                        } else {
                            // Todo
                        }
                    }
                    delay(5_000)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun loadFilter() {
        binding.apply {
            filterType.updateWeb3TokenFilterType(filterParams.tokenFilterType)
            filterAsset.updateWeb3Tokens(R.string.Assets, filterParams.tokenItems)
            filterTime.setTitle(filterParams.selectTime ?: getString(R.string.Date))
            titleView.setSubTitle(
                getString(R.string.All_Transactions), getString(
                    when (filterParams.order) {
                        SortOrder.Amount -> R.string.sort_by_amount
                        SortOrder.Value -> R.string.sort_by_value
                        SortOrder.Oldest -> R.string.sort_by_oldest
                        else -> R.string.sort_by_recent
                    }
                )
            )
            Timber.e(filterParams.toString())
            bindLiveData()
        }
    }

    override fun onApplyClick() {
        // Do noting
    }


    private fun bindLiveData() {
        bindLiveData(walletViewModel.allWeb3Transaction(initialLoadKey = initialLoadKey, filterParams))
    }

    private fun showEmpty(show: Boolean) {
        binding.apply {
            if (show) {
                if (empty.root.visibility == GONE) {
                    empty.root.visibility = VISIBLE
                }
                if (transactionsRv.visibility == VISIBLE) {
                    transactionsRv.visibility = GONE
                }
            } else {
                if (empty.root.visibility == VISIBLE) {
                    empty.root.visibility = GONE
                }
                if (transactionsRv.visibility == GONE) {
                    transactionsRv.visibility = VISIBLE
                }
            }
        }
    }

    private fun selectAsset() {
        binding.filterAsset.open()
        multiSelectWeb3TokenListBottomSheetDialogFragment
            .showNow(parentFragmentManager, MultiSelectWeb3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun dateRangePicker(): MaterialDatePicker<Pair<Long, Long>> {
        val constraints = CalendarConstraints.Builder()
            .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTheme(R.style.AppTheme_DatePicker)
            .setTitleText(getString(R.string.Select_Date))
            .setNegativeButtonText(getString(R.string.Reset))
            .apply {
                val start = filterParams.startTime ?: return@apply
                val end = filterParams.endTime ?: return@apply
                setSelection(Pair(start, end))
            }
            .setCalendarConstraints(constraints)
            .build()
        dateRangePicker.addOnDismissListener {
            binding.filterTime.close()
        }
        dateRangePicker.addOnNegativeButtonClickListener {
            filterParams.startTime = null
            filterParams.endTime = null
            loadFilter()
        }
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            filterParams.startTime = startDate
            filterParams.endTime = endDate
            loadFilter()
        }
        return dateRangePicker
    }

    private fun datePicker() {
        binding.filterTime.open()
        dateRangePicker().show(parentFragmentManager, MaterialDatePicker::class.java.name)
    }

    private val sortMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.titleView.rightIb
            setAdapter(menuAdapter)
            setOnItemClickListener { _, _, position, _ ->
                filterParams.order = when (position) {
                    0 -> SortOrder.Recent
                    1 -> SortOrder.Oldest
                    2 -> SortOrder.Value
                    else -> SortOrder.Amount
                }
                loadFilter()
                dismiss()
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.END)
            horizontalOffset = requireContext().dpToPx(2f)
            verticalOffset = requireContext().dpToPx(10f)
        }
    }

    private val menuAdapter: SortMenuAdapter by lazy {
        val menuItems = listOf(
            SortMenuData(SortOrder.Recent, R.drawable.ic_menu_recent, R.string.Recent),
            SortMenuData(SortOrder.Oldest, R.drawable.ic_menu_oldest, R.string.Oldest),
            SortMenuData(SortOrder.Value, R.drawable.ic_menu_value, R.string.Value_Descending),
            SortMenuData(SortOrder.Amount, R.drawable.ic_menu_amount, R.string.Amount_Descending),
        )
        SortMenuAdapter(requireContext(), menuItems).apply {
            checkPosition = when (filterParams.order) {
                SortOrder.Recent -> 0
                SortOrder.Oldest -> 1
                SortOrder.Amount -> 2
                else -> 3
            }
        }
    }

    private val typeMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.filterType
            setAdapter(typeAdapter)
            setOnItemClickListener { _, _, position, _ ->
                filterParams.tokenFilterType = when (position) {
                    0 -> Web3TokenFilterType.ALL
                    1 -> Web3TokenFilterType.SEND
                    else -> Web3TokenFilterType.RECEIVE
                }
                loadFilter()
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.START)
            horizontalOffset = 0
            verticalOffset = requireContext().dpToPx(10f)
            setOnDismissListener {
                binding.filterType.close()
            }
        }
    }

    private val typeAdapter: Web3TypeMenuAdapter by lazy {
        val menuItems = listOf(
            Web3TypeMenuData(Web3TokenFilterType.ALL, null, Web3TokenFilterType.ALL.titleRes),
            Web3TypeMenuData(Web3TokenFilterType.SEND, R.drawable.ic_menu_type_withdrawal, Web3TokenFilterType.SEND.titleRes),
            Web3TypeMenuData(Web3TokenFilterType.RECEIVE, R.drawable.ic_menu_type_deoisit, Web3TokenFilterType.RECEIVE.titleRes),
            Web3TypeMenuData(Web3TokenFilterType.CONTRACT, R.drawable.ic_menu_type_contract, Web3TokenFilterType.CONTRACT.titleRes),
        )
        Web3TypeMenuAdapter(requireContext(), menuItems).apply {
            checkPosition = when (filterParams.tokenFilterType) {
                Web3TokenFilterType.ALL -> 0
                Web3TokenFilterType.SEND -> 1
                Web3TokenFilterType.RECEIVE -> 2
                else -> 3
            }
        }
    }

    private val multiSelectWeb3TokenListBottomSheetDialogFragment by lazy {
        MultiSelectWeb3TokenListBottomSheetDialogFragment.newInstance()
            .setOnMultiSelectTokenListener(object : MultiSelectWeb3TokenListBottomSheetDialogFragment.OnMultiSelectTokenListener {
                override fun onTokenSelect(tokenItems: List<Web3TokenItem>?) {
                    filterParams.tokenItems = tokenItems
                    loadFilter()
                }

                override fun onDismiss() {
                    binding.filterAsset.close()
                }
            })
            .setDateProvider(object : MultiSelectWeb3TokenListBottomSheetDialogFragment.DataProvider {
                override fun getCurrentTokens(): List<Web3TokenItem> {
                    return filterParams.tokenItems ?: emptyList()
                }
            })
    }
}

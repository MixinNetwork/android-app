package one.mixin.android.ui.wallet

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAllTransactionsBinding
import one.mixin.android.databinding.ViewReputationBottomBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.PendingTransactionRefreshHelper
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.Web3FilterParams.Companion.FILTER_GOOD_AND_SPAM
import one.mixin.android.ui.wallet.Web3FilterParams.Companion.FILTER_GOOD_AND_UNKNOWN
import one.mixin.android.ui.wallet.Web3FilterParams.Companion.FILTER_MASK
import one.mixin.android.ui.wallet.adapter.Web3TransactionPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.details.Web3TransactionFragment
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

@AndroidEntryPoint
class AllWeb3TransactionsFragment : BaseTransactionsFragment<PagedList<Web3TransactionItem>>(R.layout.fragment_all_transactions) {
    companion object {
        const val TAG = "AllTransactionsFragment"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_FILTER_PARAMS = "filter_params"
    }

    private val binding by viewBinding(FragmentAllTransactionsBinding::bind)

    private val adapter = Web3TransactionPagedAdapter().apply {
        setOnItemClickListener(object : Web3TransactionPagedAdapter.OnItemClickListener {
            override fun onItemClick(transaction: Web3TransactionItem) {
                lifecycleScope.launch {
                    val token = web3ViewModel.web3TokenItemById(filterParams.walletId,transaction.getMainAssetId()) ?: return@launch
                    val wallet = web3ViewModel.findWalletById(filterParams.walletId) ?: return@launch
                    this@AllWeb3TransactionsFragment.view?.findNavController()?.navigate(
                        R.id.action_all_web3_transactions_fragment_to_web3_transaction_fragment,
                        Bundle().apply {
                            putParcelable("args_transaction", transaction)
                            putString("args_chain", transaction.chainId)
                            putParcelable("args_token", token)
                            putParcelable(Web3TransactionFragment.ARGS_WALLET, wallet.toWeb3Wallet())
                        }
                    )
                }
            }
        })
    }

    private val filterParams by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_FILTER_PARAMS, Web3FilterParams::class.java))
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
                        else -> 1
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
                    Web3TokenFilterType.RECEIVE -> 2
                    Web3TokenFilterType.APPROVAL -> 3
                    Web3TokenFilterType.SWAP -> 4
                    Web3TokenFilterType.PENDING -> 5
                }
                typeMenu.show()
            }
            filterAsset.setOnClickListener {
                selectAsset()
            }
            filterUser.isVisible = false
            filterReputation.setOnClickListener {
                val builder = BottomSheet.Builder(requireActivity())
                val viewBinding = ViewReputationBottomBinding.inflate(
                    LayoutInflater.from(ContextThemeWrapper(requireActivity(), R.style.Custom)),
                    null,
                    false
                )
                builder.setCustomView(viewBinding.root)
                val bottomSheet = builder.create()
                viewBinding.rightIv.setOnClickListener {
                    bottomSheet.dismiss()
                }

                val currentLevel = filterParams.level and FILTER_MASK
                viewBinding.checkUnknown.isChecked = currentLevel and FILTER_GOOD_AND_UNKNOWN != 0
                viewBinding.checkSpam.isChecked = currentLevel and FILTER_GOOD_AND_SPAM != 0

                viewBinding.checkUnknown.setOnCheckedChangeListener { _, isChecked ->
                    val spamChecked = viewBinding.checkSpam.isChecked
                    filterParams.level = (if (isChecked) FILTER_GOOD_AND_UNKNOWN else 0) or (if (spamChecked) FILTER_GOOD_AND_SPAM else 0)
                }
                viewBinding.checkSpam.setOnCheckedChangeListener { _, isChecked ->
                    val unknownChecked = viewBinding.checkUnknown.isChecked
                    filterParams.level = (if (unknownChecked) FILTER_GOOD_AND_UNKNOWN else 0) or (if (isChecked) FILTER_GOOD_AND_SPAM else 0)
                }

                viewBinding.resetButton.setOnClickListener {
                    filterParams.level = 0b00 // Good
                    viewBinding.checkUnknown.isChecked = false
                    viewBinding.checkSpam.isChecked = false
                }

                viewBinding.applyButton.setOnClickListener {
                    loadFilter()
                    bottomSheet.dismiss()
                }

                bottomSheet.show()
            }
            filterTime.setOnClickListener {
                datePicker()
            }
            loadFilter()
        }
        jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "AllWeb3TransactionsFragment resumed.")
        refreshJob = PendingTransactionRefreshHelper.startRefreshData(
            fragment = this,
            web3ViewModel = web3ViewModel,
            jobManager = jobManager,
            refreshJob = refreshJob
        )
    }

    override fun onPause() {
        super.onPause()
        refreshJob = PendingTransactionRefreshHelper.cancelRefreshData(refreshJob)
    }

    private fun loadFilter() {
        binding.apply {
            filterType.updateWeb3TokenFilterType(filterParams.tokenFilterType)
            filterAsset.updateWeb3Tokens(R.string.Assets, filterParams.tokenItems)
            filterTime.setTitle(filterParams.selectTime ?: getString(R.string.Date))
            filterReputation.updateLevel(getString(R.string.Reputation), filterParams.level)
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
                    else -> SortOrder.Oldest
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
        )
        SortMenuAdapter(requireContext(), menuItems).apply {
            checkPosition = when (filterParams.order) {
                SortOrder.Recent -> 0
                else -> 1
            }
        }
    }

    private val typeMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.filterType
            setAdapter(typeAdapter)
            setOnItemClickListener { _, _, position, _ ->
                filterParams.tokenFilterType = Web3TokenFilterType.fromInt(position)
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
            Web3TypeMenuData(Web3TokenFilterType.APPROVAL, R.drawable.ic_menu_type_approval, Web3TokenFilterType.APPROVAL.titleRes),
            Web3TypeMenuData(Web3TokenFilterType.SWAP, R.drawable.ic_menu_type_swap, Web3TokenFilterType.SWAP.titleRes),
            Web3TypeMenuData(Web3TokenFilterType.PENDING, R.drawable.ic_menu_type_pending, Web3TokenFilterType.PENDING.titleRes)
        )
        Web3TypeMenuAdapter(requireContext(), menuItems).apply {
            checkPosition = when (filterParams.tokenFilterType) {
                Web3TokenFilterType.ALL -> 0
                Web3TokenFilterType.SEND -> 1
                Web3TokenFilterType.RECEIVE -> 2
                Web3TokenFilterType.APPROVAL -> 3
                Web3TokenFilterType.SWAP -> 4
                Web3TokenFilterType.PENDING -> 5
            }
        }
    }

    private val multiSelectWeb3TokenListBottomSheetDialogFragment by lazy {
        MultiSelectWeb3TokenListBottomSheetDialogFragment.newInstance(walletId = filterParams.walletId)
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


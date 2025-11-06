package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.VISIBLE
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAllOrdersBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.navTo
import one.mixin.android.job.RefreshOrdersJob
import one.mixin.android.session.Session
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.trade.OrderDetailFragment
import one.mixin.android.ui.wallet.adapter.OrderPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.SpacesItemDecoration
import one.mixin.android.vo.route.OrderItem

@AndroidEntryPoint
class AllOrdersFragment : BaseTransactionsFragment<PagedList<OrderItem>>(R.layout.fragment_all_orders) {

    companion object {
        const val TAG: String = "AllOrdersFragment"
        const val ARGS_FILTER_PARAMS: String = "order_filter_params"
    }

    private val binding by viewBinding(FragmentAllOrdersBinding::bind)

    private val ordersViewModel by viewModels<OrdersViewModel>()

    private val adapter = OrderPagedAdapter()

    private val filterParams by lazy { OrderFilterParams() }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(RefreshOrdersJob())
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightAnimator.setOnClickListener { sortMenu.show() }
            }
            transactionsRv.itemAnimator = null
            transactionsRv.adapter = adapter

            val layoutManager = LinearLayoutManager(requireContext())
            transactionsRv.layoutManager = layoutManager
            transactionsRv.addItemDecoration(com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration(adapter))
            transactionsRv.addItemDecoration(SpacesItemDecoration(requireContext().dpToPx(4f), true))

            adapter.onItemClick = { order: OrderItem ->
                navTo(OrderDetailFragment.newInstance(order.orderId), OrderDetailFragment.TAG)
            }

            filterUser.visibility = VISIBLE
            filterReputation.visibility = VISIBLE
            filterUser.setOnClickListener {
                selectWallet()
            }
            filterType.setOnClickListener {
                filterType.open()
                typeMenu.show()
            }
            filterReputation.setOnClickListener {
                filterReputation.open()
                statusMenu.show()
            }
            filterAsset.setOnClickListener {
                selectAsset()
            }
            filterTime.setOnClickListener {
                datePicker()
            }
        }

        dataObserver = Observer { pagedList ->
            if (pagedList.isNotEmpty()) showEmpty(false) else showEmpty(true)
            adapter.submitList(pagedList)
        }

        loadFilter()
    }

    private fun loadFilter() {
        binding.apply {
            titleView.setSubTitle(getString(R.string.Orders), getString(
                when (filterParams.order) {
                    SortOrder.Oldest -> R.string.sort_by_oldest
                    else -> R.string.sort_by_recent
                }
            ))
            filterUser.setTitle(getString(R.string.Wallet))
            filterType.setTitle(getString(R.string.Type))
            filterReputation.setTitle(getString(R.string.Status))
            filterAsset.updateWeb3Tokens(R.string.Assets, filterParams.tokenItems)
            val dateTitle = if (filterParams.startTime == null && filterParams.endTime == null) {
                getString(R.string.Date)
            } else {
                formatDateRange(filterParams.startTime, filterParams.endTime)
            }
            filterTime.setTitle(dateTitle)
            bindLiveData()
        }
    }

    private fun bindLiveData() {
        bindLiveData(ordersViewModel.allLimitOrders(initialLoadKey = initialLoadKey, filterParams))
    }

    override fun onApplyClick() {
        // no-op
    }

    private fun showEmpty(show: Boolean) {
        binding.apply {
            if (show) {
                if (empty.root.visibility == View.GONE) empty.root.visibility = View.VISIBLE
                if (transactionsRv.visibility == View.VISIBLE) transactionsRv.visibility = View.GONE
            } else {
                if (empty.root.visibility == View.VISIBLE) empty.root.visibility = View.GONE
                if (transactionsRv.visibility == View.GONE) transactionsRv.visibility = View.VISIBLE
            }
        }
    }

    private val sortMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.titleView.rightIb
            setAdapter(SortMenuAdapter(requireContext(), listOf(
                SortMenuData(SortOrder.Recent, R.drawable.ic_menu_recent, R.string.Recent),
                SortMenuData(SortOrder.Oldest, R.drawable.ic_menu_oldest, R.string.Oldest)
            )))
            setOnItemClickListener { _, _, position, _ ->
                filterParams.order = when (position) { 0 -> SortOrder.Recent else -> SortOrder.Oldest }
                loadFilter()
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.END)
        }
    }

    private val typeMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.filterType
            verticalOffset = requireContext().dpToPx(8f)
            val rawValues = listOf<String?>(
                null, // All
                "swap",
                "limit",
            )
            val display = listOf(
                getString(R.string.All),
                getString(R.string.order_type_swap),
                getString(R.string.order_type_limit),
            )
            setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, display))
            setOnItemClickListener { _, _, position, _ ->
                filterParams.orderTypes = rawValues[position]?.let { listOf(it) }
                loadFilter()
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.START)
            setOnDismissListener { binding.filterType.close() }
        }
    }

    private val statusMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.filterReputation
            verticalOffset = requireContext().dpToPx(8f)
            val rawValues = listOf<String?>(
                null,
                "created",
                "pricing",
                "quoting",
                "settled",
                "expired",
                "cancelled",
                "failed",
            )
            val display = listOf(
                getString(R.string.All),
                getString(R.string.order_state_created),
                getString(R.string.order_state_pricing),
                getString(R.string.order_state_quoting),
                getString(R.string.Success),
                getString(R.string.Expired),
                getString(R.string.Canceled),
                getString(R.string.State_Failed)
            )
            setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, display))
            setOnItemClickListener { _, _, position, _ ->
                filterParams.statuses = rawValues[position]?.let { listOf(it) }
                loadFilter()
                dismiss()
            }
            width = requireContext().dpToPx(250f)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.START)
            setOnDismissListener { binding.filterReputation.close() }
        }
    }

    private fun selectAsset() {
        binding.filterAsset.open()
        multiSelectWeb3TokenListBottomSheetDialogFragment
            .showNow(parentFragmentManager, MultiSelectWeb3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun selectWallet() {
        binding.filterUser.open()
        WalletListBottomSheetDialogFragment.newInstance(excludeWalletId = "", chainId = null)
            .apply {
                setOnWalletClickListener { wallet ->
                    filterParams.walletIds = if (wallet == null) {
                        listOfNotNull(Session.getAccountId())
                    } else {
                        listOf(wallet.id)
                    }
                    val title = wallet?.name ?: getString(R.string.Privacy_Wallet)
                    binding.filterUser.setTitle(title)
                    loadFilter()
                    binding.filterUser.close()
                }
                setOnDismissListener {
                    binding.filterUser.close()
                }
            }
            .showNow(parentFragmentManager, WalletListBottomSheetDialogFragment.TAG)
    }

    private val multiSelectWeb3TokenListBottomSheetDialogFragment by lazy {
        MultiSelectWeb3TokenListBottomSheetDialogFragment.newInstance(walletId = Session.getAccountId())
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
        dateRangePicker.addOnDismissListener { binding.filterTime.close() }
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

    private fun formatDateRange(start: Long?, end: Long?): String {
        if (start == null || end == null) return getString(R.string.Date)
        val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        return "${df.format(java.util.Date(start))} - ${df.format(java.util.Date(end))}"
    }
}

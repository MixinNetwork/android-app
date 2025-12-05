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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.view.ViewGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
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
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.SpacesItemDecoration
import one.mixin.android.vo.route.OrderItem

@AndroidEntryPoint
class AllOrdersFragment : BaseTransactionsFragment<PagedList<OrderItem>>(R.layout.fragment_all_orders) {

    companion object {
        const val TAG: String = "AllOrdersFragment"
        private const val ARGS_WALLET_IDS: String = "args_wallet_ids"
        private const val ARGS_FILTER_PENDING: String = "args_filter_pending"

        fun newInstanceWithWalletIds(walletIds: ArrayList<String>, filterPending: Boolean = false): AllOrdersFragment {
            val f = AllOrdersFragment()
            val args = Bundle()
            args.putStringArrayList(ARGS_WALLET_IDS, walletIds)
            args.putBoolean(ARGS_FILTER_PENDING, filterPending)
            f.arguments = args
            return f
        }
    }
    private fun createMenuAdapter(items: List<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.setTextColor(resolveAttrColor(R.attr.text_primary))
                return view
            }
        }
    }

    private fun resolveAttrColor(attr: Int): Int {
        val ta = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0)
        ta.recycle()
        return color
    }

    private val binding by viewBinding(FragmentAllOrdersBinding::bind)

    private val ordersViewModel by viewModels<OrdersViewModel>()

    private val adapter = OrderPagedAdapter()

    private val filterParams by lazy { OrderFilterParams() }

    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var refreshJob: Job? = null

    private val sortAdapter by lazy {
        SortMenuAdapter(requireContext(), listOf(
            SortMenuData(SortOrder.Recent, R.drawable.ic_menu_recent, R.string.Recent),
            SortMenuData(SortOrder.Oldest, R.drawable.ic_menu_oldest, R.string.Oldest)
        )).apply {
            checkPosition = 0
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsTracker.trackTradeTransactions()
        val walletIds = arguments?.getStringArrayList(ARGS_WALLET_IDS)
        walletIds?.let { ids ->
            if (ids.isNotEmpty()) {
                filterParams.walletIds = ids
            }
        }
        val filterPending = arguments?.getBoolean(ARGS_FILTER_PENDING, false) ?: false
        if (filterPending) {
            filterParams.statuses = listOf("created", "pricing", "quoting", "pending")
        }
        jobManager.addJobInBackground(RefreshOrdersJob())
        binding.apply {
            titleView.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightAnimator.setOnClickListener {
                    sortAdapter.checkPosition = when (filterParams.order) {
                        SortOrder.Recent -> 0
                        SortOrder.Oldest -> 1
                        else -> 0
                    }
                    sortMenu.show()
                }
            }
            transactionsRv.itemAnimator = null
            transactionsRv.adapter = adapter

            val layoutManager = LinearLayoutManager(requireContext())
            transactionsRv.layoutManager = layoutManager
            transactionsRv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
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

        startPendingOrdersPolling()
    }

    private fun loadFilter() {
        binding.apply {
            titleView.setSubTitle(getString(R.string.Orders), getString(
                when (filterParams.order) {
                    SortOrder.Oldest -> R.string.sort_by_oldest
                    else -> R.string.sort_by_recent
                }
            ))
            // Update Type chip title
            val typeSet = filterParams.orderTypes?.toSet()
            val typeTitle = when {
                typeSet == null || typeSet.isEmpty() -> getString(R.string.Type)
                typeSet == setOf("swap") -> getString(R.string.order_type_swap)
                typeSet == setOf("limit") -> getString(R.string.order_type_limit)
                else -> getString(R.string.Type)
            }
            filterType.setTitle(typeTitle)
            // Update Status chip title
            val pendingSet = setOf("created", "pricing", "quoting", "pending")
            val doneSet = setOf("settled", "success")
            val otherSet = setOf("expired", "cancelled", "canceled", "failed", "refunded", "cancelling")
            val statusesSet = filterParams.statuses?.toSet()
            val statusTitle = when {
                statusesSet == null || statusesSet.isEmpty() -> getString(R.string.Status)
                statusesSet == pendingSet -> getString(R.string.State_Pending)
                statusesSet == doneSet -> getString(R.string.Done)
                statusesSet == otherSet -> getString(R.string.Other)
                else -> getString(R.string.Status)
            }
            filterReputation.setTitle(statusTitle)
            filterAsset.updateWeb3Tokens(R.string.Assets, filterParams.tokenItems)
            val dateTitle = if (filterParams.startTime == null && filterParams.endTime == null) {
                getString(R.string.Date)
            } else {
                formatDateRange(filterParams.startTime, filterParams.endTime)
            }
            filterTime.setTitle(dateTitle)
            if (filterParams.walletIds.isNullOrEmpty()) {
                filterUser.setTitle(getString(R.string.Wallets))
            } else {
                val current = filterParams.walletIds ?: emptyList()
                val accountId = Session.getAccountId()
                if (current.size == 1) {
                    val onlyId = current.first()
                    if (accountId != null && onlyId == accountId) {
                        filterUser.setPrivacyWalletIcon(getString(R.string.Privacy_Wallet))
                    } else {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val name = web3ViewModel.getWalletName(onlyId)
                            filterUser.setTitle(name ?: getString(R.string.Wallets_Selected_Count, 1))
                        }
                    }
                } else {
                    val title = getString(R.string.Wallets_Selected_Count, current.size)
                    filterUser.setTitle(title)
                }
            }
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
            setAdapter(sortAdapter)
            setOnItemClickListener { _, _, position, _ ->
                sortAdapter.checkPosition = position
                filterParams.order = when (position) { 0 -> SortOrder.Recent else -> SortOrder.Oldest }
                loadFilter()
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
            setAdapter(createMenuAdapter(display))
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
            horizontalOffset = 0
            verticalOffset = requireContext().dpToPx(10f)
            setOnDismissListener { binding.filterType.close() }
        }
    }

    private val statusMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.filterReputation
            verticalOffset = requireContext().dpToPx(8f)
            val rawValues = listOf<List<String>?>(
                null, // All
                listOf("created", "pricing", "quoting", "pending"), // Pending
                listOf("settled", "success"), // Done
                listOf("expired", "cancelled", "canceled", "failed", "refunded", "cancelling"), // Other
            )
            val display = listOf(
                getString(R.string.All),
                getString(R.string.State_Pending),
                getString(R.string.Done),
                getString(R.string.Other)
            )
            setAdapter(createMenuAdapter(display))
            setOnItemClickListener { _, _, position, _ ->
                filterParams.statuses = rawValues[position]
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
            setOnDismissListener { binding.filterReputation.close() }
        }
    }

    private fun selectAsset() {
        binding.filterAsset.open()
        MultiSelectWeb3TokenListBottomSheetDialogFragment.newInstance(filterParams.walletIds ?: emptyList())
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
            .showNow(parentFragmentManager, MultiSelectWeb3TokenListBottomSheetDialogFragment.TAG)
    }

    private fun selectWallet() {
        binding.filterUser.open()
        val accountId = Session.getAccountId()
        val currentIds = filterParams.walletIds ?: emptyList()
        val privacySelected = accountId != null && currentIds.contains(accountId)
        val initialIds = if (privacySelected) currentIds.filterNot { it == accountId } else currentIds
        WalletMultiSelectBottomSheetDialogFragment.newInstance()
            .setInitialSelection(initialIds, privacySelected)
            .setOnConfirmListener { selected ->
                val ids = selected.mapNotNull { w -> w?.id ?: accountId }
                filterParams.walletIds = ids.ifEmpty { null }
                val names = selected.map { it?.name ?: getString(R.string.Privacy_Wallet) }
                val title = when (names.size) {
                    0 -> getString(R.string.Wallets)
                    1 -> names.first()
                    else -> getString(R.string.Wallets_Selected_Count, names.size)
                }
                binding.filterUser.setTitle(title)
                loadFilter()
                binding.filterUser.close()
            }
            .setOnDismissListener {
                binding.filterUser.close()
            }
            .showNow(parentFragmentManager, WalletMultiSelectBottomSheetDialogFragment.TAG)
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

    override fun onDestroyView() {
        super.onDestroyView()
        stopPendingOrdersPolling()
    }

    private fun startPendingOrdersPolling() {
        refreshJob?.cancel()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isAdded) {
                val hadPending: Boolean = ordersViewModel.refreshPendingOrders()
                if (hadPending) delay(3000) else break
            }
        }
    }

    private fun stopPendingOrdersPolling() {
        refreshJob?.cancel()
        refreshJob = null
    }
}

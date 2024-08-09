package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentAllTransactionsBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.NonMessengerUserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
import one.mixin.android.ui.wallet.TransactionFragment.Companion.ARGS_SNAPSHOT
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.SnapshotPagedAdapter
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.notMessengerUser
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.toSnapshot
import timber.log.Timber

@AndroidEntryPoint
class AllTransactionsFragment : BaseTransactionsFragment<PagedList<SnapshotItem>>(R.layout.fragment_all_transactions), OnSnapshotListener, MultiSelectTokenListBottomSheetDialogFragment.DataProvider, MultiSelectRecipientsListBottomSheetDialogFragment.DataProvider {
    companion object {
        const val TAG = "AllTransactionsFragment"
        const val ARGS_USER = "args_user"

        fun newInstance(user:UserItem):AllTransactionsFragment {
            return AllTransactionsFragment().withArgs {
                putParcelable(ARGS_USER, user)
            }
        }
    }

    private val binding by viewBinding(FragmentAllTransactionsBinding::bind)

    private val adapter = SnapshotPagedAdapter()

    private val userItem by lazy {
        requireArguments().getParcelableCompat(ARGS_USER, UserItem::class.java)
    }

    private val filterParams by lazy {
        FilterParams(recipients = userItem?.let { listOf(it) })
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        adapter.listener = this
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
                    val opponentIds =
                        pagedList.filter {
                            !it?.opponentId.isNullOrBlank()
                        }.map {
                            it.opponentId
                        }
                    walletViewModel.checkAndRefreshUsers(opponentIds)
                } else {
                    showEmpty(true)
                }
                adapter.submitList(pagedList)
            }
        bindLiveData()
        binding.apply {
            filterType.setOnClickListener {
                filterType.open()
                typeAdapter.checkPosition = filterParams.type.value
                typeMenu.show()
            }
            filterAsset.setOnClickListener {
                selectAsset()
            }
            filterUser.setOnClickListener {
                selectUser()
            }
            filterTime.setOnClickListener {
                datePicker()
            }
            loadFilter()
        }
        refreshAllPendingDeposit()
    }

    private fun loadFilter() {
        binding.apply {
            filterType.setTitle(filterParams.typeTitle)
            filterAsset.updateTokens(R.string.Assets, filterParams.tokenItems)
            filterUser.updateUsers(R.string.Recipients, filterParams.recipients)
            filterTime.setTitle(filterParams.selectTime?:getString(R.string.Date))
            titleView.setSubTitle(getString(R.string.All_Transactions), getString(
                when(filterParams.order) {
                    SortOrder.Amount -> R.string.sort_by_amount
                    SortOrder.Value -> R.string.sort_by_value
                    SortOrder.Oldest -> R.string.sort_by_oldest
                    else -> R.string.sort_by_recent
                }
            ))
            Timber.e(filterParams.toString())
            bindLiveData()
        }
    }

    override fun <T> onNormalItemClick(item: T) {
        lifecycleScope.launch {
            val snapshot = item as SnapshotItem
            val a =
                withContext(Dispatchers.IO) {
                    walletViewModel.simpleAssetItem(snapshot.assetId)
                }
            a?.let {
                if (viewDestroyed()) return@launch

                if (requireActivity() !is WalletActivity) {
                    activity?.addFragment(
                        this@AllTransactionsFragment,
                        TransactionFragment.newInstance(snapshot, it),
                        TransactionFragment.TAG,
                    )
                } else {
                    view?.navigate(
                        R.id.action_all_transactions_fragment_to_transaction_fragment,
                        Bundle().apply {
                            putParcelable(ARGS_SNAPSHOT, snapshot)
                            putParcelable(ARGS_ASSET, it)
                        },
                    )
                }
            }
        }
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch {
            val user =
                withContext(Dispatchers.IO) {
                    walletViewModel.getUser(userId)
                } ?: return@launch
            if (user.notMessengerUser()) {
                NonMessengerUserBottomSheetDialogFragment.newInstance(user)
                    .showNow(parentFragmentManager, NonMessengerUserBottomSheetDialogFragment.TAG)
            } else {
                val f = UserBottomSheetDialogFragment.newInstance(user)
                f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun onApplyClick() {
        // Do noting
    }

    private fun refreshAllPendingDeposit() =
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = { walletViewModel.allPendingDeposit() },
                successBlock = {
                    walletViewModel.clearAllPendingDeposits()

                    val pendingDeposits = it.data
                    if (pendingDeposits.isNullOrEmpty()) {
                        return@handleMixinResponse
                    }
                    val destinationTags = walletViewModel.findDepositEntryDestinations()
                    pendingDeposits
                        .filter { pd ->
                            destinationTags.any { dt ->
                                dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                            }
                        }
                        .chunked(100) { chunk ->
                            lifecycleScope.launch {
                                chunk.map { pd ->
                                    pd.toSnapshot()
                                }.let { list ->
                                    walletViewModel.insertPendingDeposit(list)
                                }
                            }
                        }
                },
            )
        }

    private fun bindLiveData() {
        bindLiveData(walletViewModel.allSnapshots(initialLoadKey = initialLoadKey, filterParams))
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

    private val multiSelectTokenListBottomSheetDialogFragment by lazy {
        MultiSelectTokenListBottomSheetDialogFragment.newInstance()
            .setDateProvider(this@AllTransactionsFragment)
            .setOnMultiSelectTokenListener(object : MultiSelectTokenListBottomSheetDialogFragment.OnMultiSelectTokenListener {
                override fun onTokenSelect(tokenItems: List<TokenItem>?) {
                    binding.filterAsset.close()
                    filterParams.tokenItems = tokenItems
                    loadFilter()
                }

                override fun onDismiss() {
                    binding.filterAsset.close()
                }
            })
    }

    private fun selectAsset() {
        binding.filterAsset.open()
        multiSelectTokenListBottomSheetDialogFragment
            .showNow(parentFragmentManager, MultiSelectTokenListBottomSheetDialogFragment.TAG)
    }

    private val multiSelectRecipientsListBottomSheetDialogFragment by lazy {
        MultiSelectRecipientsListBottomSheetDialogFragment.newInstance(userItem)
            .setDateProvider(this@AllTransactionsFragment)
            .setOnMultiSelectUserListener(object : MultiSelectRecipientsListBottomSheetDialogFragment.OnMultiSelectRecipientListener {
                override fun onRecipientSelect(recipients: List<Recipient>?) {
                    binding.filterUser.close()
                    filterParams.recipients = recipients
                    loadFilter()
                }

                override fun onDismiss() {
                    binding.filterUser.close()
                }
            })
    }

    private fun selectUser() {
        binding.filterUser.open()
        multiSelectRecipientsListBottomSheetDialogFragment.setType(filterParams.type)
        multiSelectRecipientsListBottomSheetDialogFragment.showNow(parentFragmentManager, MultiSelectRecipientsListBottomSheetDialogFragment.TAG)
    }

    private fun dateRangePicker(): MaterialDatePicker<Pair<Long,Long>> {
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
                filterParams.type = when (position) {
                    1 -> SnapshotType.deposit
                    2 -> SnapshotType.withdrawal
                    3 -> SnapshotType.snapshot
                    else -> SnapshotType.all
                }
                if (filterParams.recipients?.isNotEmpty() == true){
                    if (filterParams.type == SnapshotType.deposit || filterParams.type == SnapshotType.withdrawal) {
                        filterParams.recipients = filterParams.recipients?.filterIsInstance<AddressItem>()
                    } else if (filterParams.type == SnapshotType.snapshot) {
                        filterParams.recipients = filterParams.recipients?.filterIsInstance<UserItem>()
                    }
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

    private val typeAdapter: TypeMenuAdapter by lazy {
        val menuItems = listOf(
            TypeMenuData(SnapshotType.all, null, R.string.All),
            TypeMenuData(SnapshotType.deposit, R.drawable.ic_menu_type_deoisit, R.string.Deposit),
            TypeMenuData(SnapshotType.withdrawal, R.drawable.ic_menu_type_withdrawal, R.string.Withdrawal),
            TypeMenuData(SnapshotType.snapshot, R.drawable.ic_menu_type_transfer, R.string.Transfer),
        )
        TypeMenuAdapter(requireContext(), menuItems).apply {
            checkPosition = filterParams.type.value
        }
    }

    override fun getCurrentTokens(): List<TokenItem> {
        return filterParams.tokenItems?: emptyList()
    }

    override fun getCurrentRecipients(): List<Recipient> {
        return filterParams.recipients?: emptyList()
    }
}

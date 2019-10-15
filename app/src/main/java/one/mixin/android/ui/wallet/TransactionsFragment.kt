package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_transactions_fragment_header.view.*
import kotlinx.android.synthetic.main.view_wallet_transactions_bottom.view.*
import kotlinx.android.synthetic.main.view_wallet_transactions_send_bottom.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getEpochNano
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigate
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putString
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.ui.address.AddressActivity
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.ui.wallet.adapter.TransactionsAdapter
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.differentProcess
import one.mixin.android.vo.toAssetItem
import one.mixin.android.vo.toSnapshot
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.doAsync
import timber.log.Timber

class TransactionsFragment : BaseTransactionsFragment<PagedList<SnapshotItem>>(), OnSnapshotListener {

    companion object {
        const val TAG = "TransactionsFragment"
        const val ARGS_ASSET = "args_asset"

        fun newInstance(asset: AssetItem): TransactionsFragment {
            val f = TransactionsFragment()
            val b = Bundle()
            b.putParcelable(ARGS_ASSET, asset)
            f.arguments = b
            return f
        }
    }

    private val adapter = TransactionsAdapter()
    private lateinit var asset: AssetItem

    private lateinit var headerView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = arguments!!.getParcelable(ARGS_ASSET)!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.title_tv.text = asset.name
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            showBottom()
        }

        headerView = layoutInflater.inflate(R.layout.view_transactions_fragment_header, transactions_rv, false)
        headerView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        headerView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        headerView.group_info_member_title_sort.setOnClickListener {
            showFiltersSheet()
        }
        headerView.top_rl.setOnClickListener {
            AssetKeyBottomSheetDialogFragment.newInstance(asset)
                .showNow(parentFragmentManager, AssetKeyBottomSheetDialogFragment.TAG)
        }
        updateHeader(headerView, asset)
        headerView.send_tv.setOnClickListener {
            showSendBottom()
        }
        headerView.receive_tv.setOnClickListener {
            asset.differentProcess({
                view?.navigate(R.id.action_transactions_to_deposit_public_key,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) })
            }, {
                view?.navigate(R.id.action_transactions_to_deposit_account,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) })
            }, {
                toast(getString(R.string.error_bad_data, ErrorHandler.BAD_DATA))
            })
        }

        adapter.listener = this
        adapter.headerView = headerView
        transactions_rv.itemAnimator = null
        transactions_rv.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        transactions_rv.adapter = adapter
        headerView.post {
            if (!isAdded) return@post

            headerView.bottom_rl.updateLayoutParams<ViewGroup.LayoutParams> {
                height = requireContext().screenHeight() - title_view.height - headerView.top_ll.height - headerView.group_info_member_title_layout.height
            }
        }

        dataObserver = Observer { pagedList ->
            if (currentType == R.id.filters_radio_all) {
                if (pagedList != null && pagedList.isNotEmpty()) {
                    lastCreatedAt = pagedList[pagedList.loadedCount - 1]?.createdAt
                    updateHeaderBottomLayout(false)
                    val opponentIds = pagedList.filter {
                        it?.opponentId != null
                    }.map {
                        it.opponentId!!
                    }
                    walletViewModel.checkAndRefreshUsers(opponentIds)
                } else {
                    updateHeaderBottomLayout(true)
                }
            } else {
                if (pagedList != null && pagedList.isNotEmpty()) {
                    lastCreatedAt = pagedList[pagedList.loadedCount - 1]?.createdAt
                    adapter.submitList(pagedList)
                    updateHeaderBottomLayout(false)
                } else {
                    updateHeaderBottomLayout(true)
                }
            }
            adapter.submitList(pagedList)
        }
        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, orderByAmount = currentOrder == R.id.sort_amount))
        doAsync {
            asset.assetId.let {
                walletViewModel.clearPendingDepositsByAssetId(it)
            }
        }
        walletViewModel.assetItem(asset.assetId).observe(this, Observer { assetItem ->
            assetItem?.let {
                asset = it
                updateHeader(headerView, it)
            }
        })

        refreshPendingDeposits(asset)
        jobManager.addJobInBackground(RefreshAssetsJob(asset.assetId))
    }

    fun updateData(list: List<Snapshot>?) {
        runInTransaction {
            list?.let { data ->
                walletViewModel.insertPendingDeposit(data)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeader(header: View, asset: AssetItem) {
        header.balance.text = try {
            if (asset.balance.toFloat() == 0f) {
                "0.00"
            } else {
                asset.balance.numberFormat()
            }
        } catch (ignored: NumberFormatException) {
            asset.balance.numberFormat()
        }
        header.symbol_tv.text = asset.symbol
        header.balance_as.text = try {
            if (asset.fiat().toFloat() == 0f) {
                "≈ ${Fiats.currencySymbol}0.00"
            } else {
                "≈ ${Fiats.currencySymbol}${asset.fiat().numberFormat2()}"
            }
        } catch (ignored: NumberFormatException) {
            "≈ ${Fiats.currencySymbol}${asset.fiat().numberFormat2()}"
        }
    }

    private fun refreshPendingDeposits(asset: AssetItem) {
        asset.differentProcess({
            walletViewModel.pendingDeposits(asset.assetId, destination = asset.destination).autoDispose(stopScope)
                .subscribe({ list ->
                    updateData(list.data?.map { it.toSnapshot(asset.assetId) })
                }, {
                    Timber.d(it)
                    ErrorHandler.handleError(it)
                })
        }, {
            walletViewModel.pendingDeposits(asset.assetId, destination = asset.destination, tag = asset.tag).autoDispose(stopScope)
                .subscribe({ list ->
                    updateData(list.data?.map { it.toSnapshot(asset.assetId) })
                }, {
                    Timber.d(it)
                    ErrorHandler.handleError(it)
                })
        }, {
            headerView.receive_tv.visibility = GONE
            headerView.receive_progress.visibility = VISIBLE
            walletViewModel.getAsset(asset.assetId).autoDispose(stopScope).subscribe({ response ->
                if (response?.isSuccess == true) {
                    headerView.receive_tv.visibility = VISIBLE
                    headerView.receive_progress.visibility = GONE
                    response.data?.let { asset ->
                        walletViewModel.upsetAsset(asset)
                        asset.toAssetItem().let { assetItem ->
                            this@TransactionsFragment.asset = assetItem
                            refreshPendingDeposits(assetItem)
                        }
                    }
                }
            }, {
                ErrorHandler.handleError(it)
            })
        })
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.hide.setText(if (asset.hidden == true) R.string.wallet_transactions_show else R.string.wallet_transactions_hide)
        view.hide.setOnClickListener {
            doAsync {
                walletViewModel.updateAssetHidden(asset.assetId, asset.hidden != true)
            }
            bottomSheet.dismiss()
            activity?.mainThreadDelayed({ activity?.onBackPressed() }, 200)
        }
        view.cancel.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    private fun showSendBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_send_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.contact.setOnClickListener {
            bottomSheet.dismiss()
            defaultSharedPreferences.putString(TransferFragment.ASSET_PREFERENCE, asset.assetId)
            this@TransactionsFragment.view?.navigate(R.id.action_transactions_to_single_friend_select)
        }
        view.address.setOnClickListener {
            bottomSheet.dismiss()
            AddressActivity.show(requireContext(), false, asset)
        }
        view.send_cancel.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    override fun <T> onNormalItemClick(item: T) {
        view?.navigate(R.id.action_transactions_fragment_to_transaction_fragment,
            Bundle().apply {
                putParcelable(TransactionFragment.ARGS_SNAPSHOT, item as SnapshotItem)
                putParcelable(ARGS_ASSET, asset)
            })
    }

    override fun onUserClick(userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            walletViewModel.getUser(userId)?.let {
                val f = UserBottomSheetDialogFragment.newInstance(it)
                f.showUserTransactionAction = {
                    view?.navigate(R.id.action_transactions_to_user_transactions,
                        Bundle().apply { putString(Constants.ARGS_USER_ID, userId) })
                }
                f.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    override fun refreshSnapshots() {
        jobManager.addJobInBackground(RefreshSnapshotsJob(asset.assetId, lastCreatedAt?.getEpochNano()
            ?: nowInUtc().getEpochNano(), LIMIT))
    }

    override fun onApplyClick() {
        val orderByAmount = currentOrder == R.id.sort_amount
        when (currentType) {
            R.id.filters_radio_all -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.wallet_transactions_title)
                headerView.wallet_transactions_empty.setText(R.string.wallet_transactions_empty)
            }
            R.id.filters_radio_transfer -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.transfer.name, SnapshotType.pending.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.filters_transfer)
                headerView.wallet_transactions_empty.setText(R.string.wallet_transactions_empty)
            }
            R.id.filters_radio_deposit -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.deposit.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.filters_deposit)
                headerView.wallet_transactions_empty.setText(R.string.wallet_deposits_empty)
            }
            R.id.filters_radio_withdrawal -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.withdrawal.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.filters_withdrawal)
                headerView.wallet_transactions_empty.setText(R.string.wallet_withdrawals_empty)
            }
            R.id.filters_radio_fee -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.fee.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.filters_fee)
                headerView.wallet_transactions_empty.setText(R.string.wallet_fees_empty)
            }
            R.id.filters_radio_rebate -> {
                bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.rebate.name, initialLoadKey = initialLoadKey, orderByAmount = orderByAmount))
                headerView.group_info_member_title.setText(R.string.filters_rebate)
                headerView.wallet_transactions_empty.setText(R.string.wallet_rebates_empty)
            }
        }
        filtersSheet.dismiss()
    }

    private fun updateHeaderBottomLayout(expand: Boolean) {
        headerView.bottom_rl.visibility = if (expand) VISIBLE else GONE
    }
}

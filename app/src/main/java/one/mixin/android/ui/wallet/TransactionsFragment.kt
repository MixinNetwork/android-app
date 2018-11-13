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
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.room.Transaction
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_transaction_filters.view.*
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_transactions_fragment_header.view.*
import kotlinx.android.synthetic.main.view_wallet_transactions_bottom.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.displayHeight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putString
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.wallet.adapter.TransactionsAdapter
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.differentProcess
import one.mixin.android.vo.toAssetItem
import one.mixin.android.vo.toSnapshot
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.RadioGroup
import org.jetbrains.anko.doAsync
import timber.log.Timber
import javax.inject.Inject

class TransactionsFragment : BaseFragment(), TransactionsAdapter.OnTransactionsListener {

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

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private var snapshots = listOf<SnapshotItem>()
    private lateinit var adapter: TransactionsAdapter
    private lateinit var asset: AssetItem

    private lateinit var headerView: View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        asset = arguments!!.getParcelable(ARGS_ASSET) as AssetItem
        title_view.title_tv.text = asset.name
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            showBottom()
        }

        headerView = layoutInflater.inflate(R.layout.view_transactions_fragment_header, recycler_view, false)
        headerView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        headerView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        headerView.group_info_member_title_sort.setOnClickListener {
            showFiltersSheet()
        }
        updateHeader(headerView, asset)
        headerView.tranfer_tv.setOnClickListener {
            defaultSharedPreferences.putString(TransferFragment.ASSERT_PREFERENCE, asset.assetId)
            view!!.findNavController().navigate(R.id.action_transactions_to_single_friend_select)
        }
        headerView.deposit_tv.setOnClickListener {
            asset.differentProcess({
                view!!.findNavController().navigate(R.id.action_transactions_to_deposit_public_key,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) })
            }, {
                view!!.findNavController().navigate(R.id.action_transactions_to_deposit_account,
                    Bundle().apply { putParcelable(ARGS_ASSET, asset) })
            }, {
                toast(getString(R.string.error_bad_data, ErrorHandler.BAD_DATA))
            })
        }

        adapter = TransactionsAdapter(asset).apply { data = snapshots }
        adapter.listener = this
        adapter.headerView = headerView
        recycler_view.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        recycler_view.adapter = adapter
        headerView.post {
            if (!isAdded) return@post

            headerView.bottom_rl.updateLayoutParams<ViewGroup.LayoutParams> {
                height = requireContext().displayHeight() - title_view.height - headerView.top_ll.height - headerView.group_info_member_title_layout.height
            }
        }

        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId))
        doAsync {
            asset.assetId.let { it ->
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
        jobManager.addJobInBackground(RefreshSnapshotsJob(asset.assetId))
    }

    @Transaction
    fun updateData(list: List<Snapshot>?) {
        list?.let { data ->
            walletViewModel.insertPendingDeposit(data)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeader(header: View, asset: AssetItem) {
        header.balance.text = asset.balance.numberFormat()
        header.symbol_tv.text = asset.symbol
        header.balance_as.text = "≈ $${asset.usd().numberFormat2()}"
    }

    private fun refreshPendingDeposits(asset: AssetItem) {
        asset.differentProcess({
            walletViewModel.pendingDeposits(asset.assetId, key = asset.publicKey).autoDisposable(scopeProvider)
                .subscribe({ list ->
                    updateData(list.data?.map { it.toSnapshot(asset.assetId) })
                }, {
                    Timber.d(it)
                    ErrorHandler.handleError(it)
                })
        }, {
            walletViewModel.pendingDeposits(asset.assetId, name = asset.accountName, tag = asset.accountTag).autoDisposable(scopeProvider)
                .subscribe({ list ->
                    updateData(list.data?.map { it.toSnapshot(asset.assetId) })
                }, {
                    Timber.d(it)
                    ErrorHandler.handleError(it)
                })
        }, {
            walletViewModel.getAsset(asset.assetId).autoDisposable(scopeProvider).subscribe({ response ->
                if (response?.isSuccess == true) {
                    response.data?.let { asset ->
                        asset.toAssetItem().let { assetItem ->
                            assetItem.differentProcess({
                                refreshPendingDeposits(assetItem)
                            }, {
                                refreshPendingDeposits(assetItem)
                            }, {})
                        }
                    }
                }
            }, {
                ErrorHandler.handleError(it)
            })
        })
    }

    private fun showPB(asset: AssetItem): Boolean {
        if (asset.publicKey.isNullOrEmpty()) {
            if (asset.accountName.isNullOrEmpty() || asset.accountTag.isNullOrEmpty()) {
                return true
            }
        } else {
            if (!asset.accountName.isNullOrEmpty() || !asset.accountTag.isNullOrEmpty()) {
                return true
            }
        }
        return false
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_transactions_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.withdrawal.setOnClickListener {
            bottomSheet.dismiss()
            this@TransactionsFragment.view!!.findNavController().navigate(R.id.action_transactions_to_withdrawal,
                Bundle().apply { putParcelable(ARGS_ASSET, asset) })
        }
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

    override fun <T> onNormalItemClick(item: T) {
        view!!.findNavController().navigate(R.id.action_transactions_fragment_to_transaction_fragment,
            Bundle().apply {
                putParcelable(TransactionFragment.ARGS_SNAPSHOT, item as SnapshotItem)
                putParcelable(ARGS_ASSET, asset)
            })
    }

    override fun onUserClick(userId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            walletViewModel.getUser(userId)?.let {
                UserBottomSheetDialogFragment.newInstance(it).show(requireFragmentManager(), UserBottomSheetDialogFragment.TAG)
            }
        }
    }

    private fun showFiltersSheet() {
        filtersView.filters_radio_group.setCheckedById(currentType)
        filtersSheet.show()
    }

    private val filtersSheet: BottomSheet by lazy {
        val builder = BottomSheet.Builder(requireActivity())
        val bottomSheet = builder.create()
        builder.setCustomView(filtersView)
        bottomSheet
    }

    private val filtersView: View by lazy {
        val view = View.inflate(ContextThemeWrapper(context, R.style.Custom), R.layout.fragment_transaction_filters, null)
        view.filters_title.left_ib.setOnClickListener { filtersSheet.dismiss() }
        view.filters_radio_group.setOnCheckedListener(object : RadioGroup.OnCheckedListener {
            override fun onChecked(id: Int) {
                currentType = id
                when (currentType) {
                    R.id.filters_radio_all -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId))
                        headerView.group_info_member_title.setText(R.string.wallet_transactions_title)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_transactions_empty)
                    }
                    R.id.filters_radio_transfer -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.transfer.name, SnapshotType.pending.name))
                        headerView.group_info_member_title.setText(R.string.filters_transfer)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_transactions_empty)
                    }
                    R.id.filters_radio_deposit -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.deposit.name))
                        headerView.group_info_member_title.setText(R.string.filters_deposit)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_deposits_empty)
                    }
                    R.id.filters_radio_withdrawal -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.withdrawal.name))
                        headerView.group_info_member_title.setText(R.string.filters_withdrawal)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_withdrawals_empty)
                    }
                    R.id.filters_radio_fee -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.fee.name))
                        headerView.group_info_member_title.setText(R.string.filters_fee)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_fees_empty)
                    }
                    R.id.filters_radio_rebate -> {
                        bindLiveData(walletViewModel.snapshotsFromDb(asset.assetId, SnapshotType.rebate.name))
                        headerView.group_info_member_title.setText(R.string.filters_rebate)
                        headerView.wallet_transactions_empty.setText(R.string.wallet_rebates_empty)
                    }
                }
                filtersSheet.dismiss()
            }
        })
        view
    }

    private var currentLiveData: LiveData<List<SnapshotItem>>? = null
    private fun bindLiveData(liveData: LiveData<List<SnapshotItem>>) {
        currentLiveData?.removeObserver(dataObserver)
        currentLiveData = liveData
        currentLiveData?.observe(this, dataObserver)
    }

    private val dataObserver by lazy {
        Observer<List<SnapshotItem>> { list ->
            if (currentType == R.id.filters_radio_all) {
                if (list != null && list.isNotEmpty()) {
                    updateHeaderBottomLayout(false)
                    snapshots = list
                    doAsync {
                        for (s in snapshots) {
                            s.opponentId?.let {
                                val u = walletViewModel.getUserById(it)
                                if (u == null) {
                                    jobManager.addJobInBackground(RefreshUserJob(arrayListOf(it)))
                                }
                            }
                        }
                    }
                } else {
                    updateHeaderBottomLayout(true)
                }
            } else {
                if (list != null && list.isNotEmpty()) {
                    updateHeaderBottomLayout(false)
                } else {
                    updateHeaderBottomLayout(true)
                }
            }
            adapter.data = list
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateHeaderBottomLayout(expand: Boolean) {
        headerView.bottom_rl.visibility = if (expand) VISIBLE else GONE
    }

    private var currentType = R.id.filters_radio_all
}
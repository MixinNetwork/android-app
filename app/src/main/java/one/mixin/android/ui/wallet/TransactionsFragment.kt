package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_transactions.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_transactions_fragment_header.view.*
import kotlinx.android.synthetic.main.view_wallet_transactions_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.TransactionsAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.doAsync
import javax.inject.Inject

class TransactionsFragment : BaseFragment(), TransactionsAdapter.TransactionsListener {

    companion object {
        const val TAG = "TransactionsFragment"
        const val ARGS_ASSET = "args_asset"

        const val POS_PB = 0
        const val POS_TEXT = 1

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transactions, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        asset = arguments!!.getParcelable(ARGS_ASSET) as AssetItem
        title_view.title_tv.text = asset.name
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener { showBottom() }

        val header = layoutInflater.inflate(R.layout.view_transactions_fragment_header, recycler_view, false)
        header.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        header.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        updateHeader(header, asset)
        header.deposit_tv.setOnClickListener {
            if (asset.accountName == null) {
                activity?.addFragment(this@TransactionsFragment, AddressFragment.newInstance(asset), AddressFragment.TAG)
            } else {
                activity?.addFragment(this@TransactionsFragment, DepositFragment.newInstance(asset), DepositFragment.TAG)
            }
        }

        adapter = TransactionsAdapter(snapshots, asset)
        adapter.setListener(this)
        adapter.header = header
        recycler_view.addItemDecoration(SpaceItemDecoration(1))
        recycler_view.adapter = adapter

        walletViewModel.snapshotsFromDb(asset.assetId).observe(this, Observer {
            it?.let {
                snapshots = it
                adapter.snapshots = snapshots
                adapter.notifyDataSetChanged()

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
            }
        })
        walletViewModel.assetItem(asset.assetId).observe(this, Observer {
            it?.let {
                asset = it
                updateHeader(header, it)
            }
        })

        jobManager.addJobInBackground(RefreshAssetsJob(asset.assetId))
        jobManager.addJobInBackground(RefreshSnapshotsJob(asset.assetId))
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeader(header: View, asset: AssetItem) {
        header.balance.text = asset.balance.numberFormat() + " " + asset.symbol
        header.balance_as.text = getString(R.string.wallet_unit_usd, "≈ ${asset.usd().numberFormat2()}")
        header.deposit_animator.displayedChild = if (asset.publicKey.isEmpty()) POS_PB else POS_TEXT
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        context?.let {
            val builder = BottomSheet.Builder(it)
            val view = LayoutInflater.from(it).inflate(R.layout.view_wallet_transactions_bottom, null, false)
            builder.setCustomView(view)
            val bottomSheet = builder.create()
            view.withdrawal.setOnClickListener {
                bottomSheet.dismiss()
                activity?.addFragment(this@TransactionsFragment,
                    WithdrawalFragment.newInstance(asset), WithdrawalFragment.TAG)
            }
            view.hide.setText(if (asset.hidden == true) R.string.wallet_transactions_show
            else R.string.wallet_transactions_hide)
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
    }

    override fun onItemClick(snapshot: SnapshotItem) {
        val fragment = TransactionFragment.newInstance(snapshot, asset)
        activity?.addFragment(this@TransactionsFragment, fragment, TransactionFragment.TAG)
    }
}
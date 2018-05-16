package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_bottom.view.*
import kotlinx.android.synthetic.main.view_wallet_fragment_header.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.*
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.AssetAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PieItemView
import one.mixin.android.widget.PieView
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import java.math.BigDecimal
import java.util.Collections
import javax.inject.Inject

class WalletFragment : BaseFragment(), AssetAdapter.AssetsListener {

    companion object {
        const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment = WalletFragment()
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    private var assets: List<AssetItem> = listOf()
    private val assetsAdapter: AssetAdapter = AssetAdapter(assets)
    private lateinit var header: View

    private var animated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_animator.setOnClickListener { activity?.onBackPressed() }
        title_view.left_ib.setOnClickListener { showBottom() }

        header = LayoutInflater.from(context!!).inflate(R.layout.view_wallet_fragment_header, coins_rv, false)
        assetsAdapter.setHeader(header)
        assetsAdapter.setAssetListener(this)
        coins_rv.adapter = assetsAdapter
        coins_rv.setHasFixedSize(true)
        coins_rv.addItemDecoration(SpaceItemDecoration(1))

        walletViewModel.assetItems().observe(this, android.arch.lifecycle.Observer { r: List<AssetItem>? ->
            r?.let {
                assets = r
                assetsAdapter.assets = assets.filter { it.hidden != true }
                assetsAdapter.notifyDataSetChanged()

                var totalBTC = BigDecimal(0)
                var totalUSD = BigDecimal(0)
                r.map {
                    totalBTC += it.btc()
                    totalUSD += it.usd()
                }

                header.total_as_tv.text = getString(R.string.wallet_unit_btc, totalBTC.toString().max8().numberFormat())
                header.total_tv.text = getString(R.string.wallet_unit_usd, totalUSD.numberFormat2())

                if (totalUSD.compareTo(BigDecimal.ZERO) == 0) return@Observer

                val list = r.filter { BigDecimal(it.balance).compareTo(BigDecimal.ZERO) != 0 }
                    .map { PieView.PieItem(it.symbol, (it.usd() / totalUSD).toFloat()) }
                if (list.isNotEmpty()) {
                    header.pie_item_container.removeAllViews()
                    Collections.sort(list, { o1, o2 -> ((o2.percent - o1.percent) * 100).toInt() })
                    context?.mainThreadDelayed({
                        header.pie_view.setPieItem(list, !animated)
                        animated = true
                    }, 400)

                    when (list.size) {
                        1 -> {
                            val p = list[0]
                            addItem(p, 0)
                        }
                        2 -> {
                            for (i in 0 until 2) {
                                val p = list[i]
                                addItem(p, i)
                            }
                        }
                        3 -> {
                            for (i in 0 until 3) {
                                val p = list[i]
                                addItem(p, i)
                            }
                        }
                        else -> {
                            var pre = 0
                            for (i in 0 until 2) {
                                val p = list[i]
                                addItem(p, i)
                                pre += (p.percent * 100).toInt()
                            }
                            val other = (100 - pre) / 100f
                            val item = PieItemView(context!!)
                            item.setPieItem(PieView.PieItem(getString(R.string.other), other), 2)
                            header.pie_item_container.addView(item)
                        }
                    }

                    header.pie_item_container.visibility = VISIBLE
                } else {
                    header.pie_view.setPieItem(listOf(), !animated)
                }
            }
            if (r != null && r.isEmpty()) {
                header.pie_view.setPieItem(listOf(), !animated)
            }
        })
        jobManager.addJobInBackground(RefreshAssetsJob())
    }

    override fun onResume() {
        super.onResume()
        checkPin()
    }

    private fun checkPin() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_CHECK, 0)
        var interval = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_INTERVAL, 0)
        if (last != 0L && interval == 0L) { // version until 0.3.0
            interval = Constants.INTERVAL_24_HOURS
        }
        if (cur - last > interval) {
            val pinCheckDialog = PinCheckDialogFragment.newInstance()
            pinCheckDialog.show(activity?.supportFragmentManager, PinCheckDialogFragment.TAG)
        }
    }

    private fun addItem(p: PieView.PieItem, i: Int) {
        val item = PieItemView(context!!)
        item.setPieItem(p, i)
        header.pie_item_container.addView(item)
    }

    @SuppressLint("InflateParams")
    private fun showBottom() {
        context?.let {
            val builder = BottomSheet.Builder(it)
            val view = LayoutInflater.from(it).inflate(R.layout.view_wallet_bottom, null, false)
            builder.setCustomView(view)
            val bottomSheet = builder.create()
            view.hide.setOnClickListener {
                activity?.addFragment(this@WalletFragment, HiddenAssetsFragment.newInstance(), HiddenAssetsFragment.TAG)
                bottomSheet.dismiss()
            }
            view.password.setOnClickListener {
                activity?.supportFragmentManager?.inTransaction {
                    setCustomAnimations(R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                        .add(R.id.container, OldPasswordFragment.newInstance(), OldPasswordFragment.TAG)
                        .addToBackStack(null)
                }
                bottomSheet.dismiss()
            }
            view.transactions_tv.setOnClickListener {
                activity?.addFragment(this@WalletFragment, AllTransactionsFragment.newInstance(), AllTransactionsFragment.TAG)
                bottomSheet.dismiss()
            }
            view.cancel.setOnClickListener { bottomSheet.dismiss() }

            bottomSheet.show()
        }
    }

    override fun onAsset(asset: AssetItem) {
        activity?.addFragment(this@WalletFragment, TransactionsFragment.newInstance(asset), TransactionsFragment.TAG)
    }
}

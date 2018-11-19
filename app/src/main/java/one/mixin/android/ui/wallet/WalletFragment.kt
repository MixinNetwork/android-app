package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.android.synthetic.main.view_wallet_bottom.view.*
import kotlinx.android.synthetic.main.view_wallet_fragment_header.view.*
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.putLong
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.AssetAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.PieItemView
import one.mixin.android.widget.PieView
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WalletFragment : BaseFragment(), HeaderAdapter.OnItemListener {

    companion object {
        const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment = WalletFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    private var assets: List<AssetItem> = listOf()
    private val assetsAdapter by lazy { AssetAdapter(coins_rv) }
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
        assetsAdapter.headerView = header
        assetsAdapter.onItemListener = this
        coins_rv.adapter = assetsAdapter
        (coins_rv.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        coins_rv.setHasFixedSize(true)
        coins_rv.addItemDecoration(SpaceItemDecoration(1))

        walletViewModel.assetItems().observe(this, Observer { r: List<AssetItem>? ->
            if (r == null || r.isEmpty()) {
                header.pie_view.setPieItem(listOf(), !animated)
            } else {
                assets = r
                assetsAdapter.setAssetList(r.filter { it.hidden != true })

                var totalBTC = BigDecimal(0)
                var totalUSD = BigDecimal(0)
                r.map {
                    totalBTC += it.btc()
                    totalUSD += it.usd()
                }

                header.total_as_tv.text = getString(R.string.wallet_unit_btc, totalBTC.numberFormat8())
                header.total_tv.text = getString(R.string.wallet_unit_usd, totalUSD.numberFormat2())

                if (totalUSD.compareTo(BigDecimal.ZERO) == 0) return@Observer

                setPieView(r, totalUSD)
            }
        })

        checkPin()
    }

    private fun setPieView(r: List<AssetItem>, totalUSD: BigDecimal) {
        val list = r.asSequence().filter { BigDecimal(it.balance).compareTo(BigDecimal.ZERO) != 0 }.map {
            val p = (it.usd() / totalUSD).setScale(2, RoundingMode.DOWN).toFloat()
            PieView.PieItem(it.symbol, p)
        }.toMutableList()
        if (list.isNotEmpty()) {
            header.pie_item_container.removeAllViews()
            list.sortWith(Comparator { o1, o2 -> ((o2.percent - o1.percent) * 100).toInt() })
            context?.mainThreadDelayed({
                header.pie_view.setPieItem(list, !animated)
                animated = true
            }, 400)

            when (list.size) {
                1 -> {
                    val p = list[0]
                    addItem(PieView.PieItem(p.name, 1f), 0)
                }
                2 -> {
                    addItem(list[0], 0)
                    val p1 = list[1]
                    val newP1 = PieView.PieItem(p1.name, 1 - p1.percent)
                    addItem(newP1, 1)
                }
                3 -> {
                    addItem(list[0], 0)
                    addItem(list[1], 1)
                    val p2 = list[2]
                    val p2Percent = 1 - list[0].percent - list[1].percent
                    val newP2 = PieView.PieItem(p2.name, p2Percent)
                    addItem(newP2, 2)
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

    private fun checkPin() {
        val cur = System.currentTimeMillis()
        val last = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_CHECK, 0)
        var interval = defaultSharedPreferences.getLong(Constants.Account.PREF_PIN_INTERVAL, 0)
        val account = Session.getAccount()
        if (account != null && account.hasPin && last == 0L) {
            interval = Constants.INTERVAL_24_HOURS
            defaultSharedPreferences.putLong(Constants.Account.PREF_PIN_INTERVAL, Constants.INTERVAL_24_HOURS)
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
        val builder = BottomSheet.Builder(requireActivity())
        val view = View.inflate(ContextThemeWrapper(requireActivity(), R.style.Custom), R.layout.view_wallet_bottom, null)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.hide.setOnClickListener {
            activity?.addFragment(this@WalletFragment, HiddenAssetsFragment.newInstance(), HiddenAssetsFragment.TAG)
            bottomSheet.dismiss()
        }
        view.setting.setOnClickListener {
            activity?.supportFragmentManager?.inTransaction {
                setCustomAnimations(R.anim.slide_in_bottom,
                    R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .add(R.id.container, WalletSettingFragment.newInstance(), WalletSettingFragment.TAG)
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

    override fun <T> onNormalItemClick(item: T) {
        activity?.addFragment(this@WalletFragment, TransactionsFragment.newInstance(item as AssetItem), TransactionsFragment.TAG)
    }
}

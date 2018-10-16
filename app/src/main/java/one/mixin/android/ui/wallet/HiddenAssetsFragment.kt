package one.mixin.android.ui.wallet

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_hidden_assets.*
import kotlinx.android.synthetic.main.view_title.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.common.itemdecoration.SpaceItemDecoration
import one.mixin.android.ui.wallet.adapter.AssetAdapter
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

class HiddenAssetsFragment : BaseFragment(), HeaderAdapter.OnItemListener {

    companion object {
        val TAG = HiddenAssetsFragment::class.java.simpleName!!
        fun newInstance() = HiddenAssetsFragment()

        const val POS_ASSET = 0
        const val POS_EMPTY = 1
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }
    private var assets: List<AssetItem> = listOf()
    private val assetsAdapter by lazy { AssetAdapter(assets_rv) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_hidden_assets, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        left_ib.setOnClickListener { activity?.onBackPressed() }
        assetsAdapter.onItemListener = this
        assets_rv.adapter = assetsAdapter
        assets_rv.addItemDecoration(SpaceItemDecoration())
        walletViewModel.hiddenAssets().observe(this, Observer {
            if (it != null && it.isNotEmpty()) {
                assets_va.displayedChild = POS_ASSET
                assets = it
                assetsAdapter.setAssetList(it)
            } else {
                assets_va.displayedChild = POS_EMPTY
            }
        })
    }

    override fun <T> onNormalItemClick(item: T) {
        activity?.addFragment(this@HiddenAssetsFragment,
            TransactionsFragment.newInstance(item as AssetItem), TransactionsFragment.TAG)
    }
}
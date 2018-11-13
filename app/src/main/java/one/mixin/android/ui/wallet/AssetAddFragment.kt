package one.mixin.android.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.fragment_asset_add.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.adapter.AssetAddAdapter
import one.mixin.android.vo.HotAsset
import one.mixin.android.widget.SearchView
import org.jetbrains.anko.textColor
import javax.inject.Inject

class AssetAddFragment : BaseFragment() {
    companion object {
        const val POS_RV = 0
        const val POS_PB = 1
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private val onHotAssetListener = object : AssetAddAdapter.OnHotAssetListener {
        override fun onItemClick(hotAsset: HotAsset, isChecked: Boolean) {
            if (isChecked) {
                adapter.checkedAssets[hotAsset.assetId] = hotAsset
                if (!search_et.text.isNullOrBlank()) {
                    searchCheckedAssetIds.add(hotAsset.assetId)
                }
            } else {
                adapter.checkedAssets.remove(hotAsset.assetId, hotAsset)
            }
            checkTitle()
        }
    }
    private val adapter = AssetAddAdapter()

    private val searchCheckedAssetIds = ArraySet<String>()
    private var hopAssets: List<HotAsset>? = null
    private var currentSearch: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_asset_add, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            search_et?.hideKeyboard()
            view!!.findNavController().navigateUp()
        }
        title_view.right_animator.isEnabled = false
        title_view.right_animator.setOnClickListener {
            walletViewModel.saveAssets(adapter.checkedAssets.values.toList())
            search_et?.hideKeyboard()
            view!!.findNavController().navigateUp()
        }
        adapter.onHotAssetListener = onHotAssetListener
        assets_rv.adapter = adapter
        search_et.listener = object : SearchView.OnSearchViewListener {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    currentSearch?.cancel()
                    showHot()
                    searchCheckedAssetIds.forEach {
                        if (adapter.checkedAssets.contains(it)) {
                            adapter.checkedAssets.remove(it)
                        }
                    }
                }
            }

            override fun onSearch() {
                search()
            }
        }

        walletViewModel.observeHotAssets().observe(this, Observer {
            hopAssets = it
            showHot()
        })
        walletViewModel.refreshHotAssets()
    }

    override fun onStop() {
        super.onStop()
        currentSearch?.cancel()
    }

    private fun checkTitle() {
        if (adapter.checkedAssets.isEmpty()) {
            title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
            title_view.right_animator.isEnabled = false
        } else {
            title_view.right_tv.textColor = resources.getColor(R.color.wallet_blue_secondary, null)
            title_view.right_animator.isEnabled = true
        }
    }

    private fun showHot() {
        adapter.submitList(hopAssets)
        va.displayedChild = POS_RV
    }

    private fun search() {
        currentSearch?.cancel()

        val query = search_et.text.toString()
        adapter.submitList(null)
        if (va.displayedChild != POS_PB) {
            va.displayedChild = POS_PB
        }
        currentSearch = GlobalScope.launch(Dispatchers.IO) {
            val pair = walletViewModel.queryAsset(query)
            launch(Dispatchers.Main) {
                adapter.existsSet = pair.second
                adapter.submitList(pair.first)
                va.displayedChild = POS_RV
            }
        }
    }
}
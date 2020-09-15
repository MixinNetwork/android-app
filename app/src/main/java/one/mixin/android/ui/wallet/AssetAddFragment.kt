package one.mixin.android.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_asset_add.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.adapter.AssetAddAdapter
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.TopAssetItem
import one.mixin.android.widget.SearchView
import org.jetbrains.anko.textColor
import javax.inject.Inject

@AndroidEntryPoint
class AssetAddFragment : BaseFragment() {
    companion object {
        const val POS_RV = 0
        const val POS_PB = 1
        const val POS_EMPTY = 2
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private val onTopAssetListener = object : AssetAddAdapter.OnTopAssetListener {
        override fun onItemClick(topAsset: TopAssetItem, isChecked: Boolean) {
            if (isChecked) {
                adapter.checkedAssets[topAsset.assetId] = topAsset
                if (!search_et.text.isNullOrBlank()) {
                    searchCheckedAssetIds.add(topAsset.assetId)
                }
            } else {
                try {
                    adapter.checkedAssets.remove(topAsset.assetId, topAsset)
                } catch (e: NoSuchMethodError) {
                    // Samsung Galaxy Note4 Android M
                    adapter.checkedAssets.remove(topAsset.assetId)
                }
            }
            checkTitle()
        }

        override fun onHiddenClick(assetItem: AssetItem) {
            alertDialogBuilder()
                .setMessage(getString(R.string.wallet_add_asset_already_hidden, assetItem.symbol))
                .setPositiveButton(R.string.wallet_transactions_show) { _, _ ->
                    lifecycleScope.launch {
                        walletViewModel.updateAssetHidden(assetItem.assetId, false)
                        adapter.existsSet?.forEach {
                            if (it.assetId == assetItem.assetId) {
                                it.hidden = false
                            }
                        }

                        assets_rv?.let {
                            Snackbar.make(it, getString(R.string.wallet_already_shown, assetItem.symbol), Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
    private val adapter = AssetAddAdapter()

    private val searchCheckedAssetIds = ArraySet<String>()
    private var topAssets: List<TopAssetItem>? = null
    private var currentSearch: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_asset_add, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            search_et?.hideKeyboard()
            view.findNavController().navigateUp()
        }
        title_view.right_animator.isEnabled = false
        title_view.right_animator.setOnClickListener {
            walletViewModel.saveAssets(adapter.checkedAssets.values.toList())
            context?.toast(R.string.successful)
            search_et?.hideKeyboard()
            view.findNavController().navigateUp()
        }
        adapter.onTopAssetListener = onTopAssetListener
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
                val query = search_et.text.toString()
                if (query.isBlank()) return

                assets_rv.removeAllViewsInLayout()

                search(query)
            }
        }

        walletViewModel.observeTopAssets().observe(
            viewLifecycleOwner,
            {
                topAssets = it
                if (search_et.text.isNullOrBlank()) {
                    showHot()
                }
            }
        )
        walletViewModel.refreshHotAssets()
    }

    override fun onStop() {
        super.onStop()
        currentSearch?.cancel()
    }

    private fun checkTitle() {
        if (adapter.checkedAssets.isEmpty) {
            title_view.right_tv.textColor = resources.getColor(R.color.text_gray, null)
            title_view.right_animator.isEnabled = false
        } else {
            title_view.right_tv.textColor = resources.getColor(R.color.wallet_blue_secondary, null)
            title_view.right_animator.isEnabled = true
        }
    }

    private fun showHot() {
        adapter.submitList(topAssets)
        va.displayedChild = POS_RV
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch = lifecycleScope.launch {
            if (!isAdded) return@launch

            adapter.submitList(null)
            if (va.displayedChild != POS_PB) {
                va.displayedChild = POS_PB
            }
            val pair = walletViewModel.queryAsset(query)
            adapter.existsSet = pair.second
            adapter.submitList(pair.first)
            if (pair.first.isNullOrEmpty()) {
                va.displayedChild = POS_EMPTY
            } else {
                va.displayedChild = POS_RV
            }
        }
    }
}

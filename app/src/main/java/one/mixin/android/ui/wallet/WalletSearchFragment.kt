package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_wallet_search.*
import kotlinx.android.synthetic.main.fragment_wallet_search.search_et
import kotlinx.android.synthetic.main.fragment_wallet_search.search_rv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navigate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.SearchAdapter
import one.mixin.android.ui.wallet.adapter.SearchDefaultAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCallback
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.TopAssetItem
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WalletSearchFragment : BaseFragment() {
    companion object {
        const val POS_DEFAULT = 0
        const val POS_SEARCH = 1
    }

    private val viewModel by viewModels<WalletViewModel>()

    private val searchDefaultAdapter by lazy {
        SearchDefaultAdapter()
    }
    private val searchAdapter by lazy {
        SearchAdapter()
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_wallet_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_ib.setOnClickListener {
            search_et.hideKeyboard()
            activity?.onBackPressed()
        }
        search_et.hint = getString(R.string.wallet_search_hint)
        @SuppressLint("AutoDispose")
        disposable = search_et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    if (it.isNullOrBlank()) {
                        rv_va?.displayedChild = POS_DEFAULT
                    } else {
                        rv_va?.displayedChild = POS_SEARCH
                        search(it.toString())
                    }
                },
                {}
            )

        default_rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val decoration by lazy { StickyRecyclerHeadersDecoration(searchDefaultAdapter) }
        default_rv.addItemDecoration(decoration)
        default_rv.adapter = searchDefaultAdapter

        search_rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        search_rv.adapter = searchAdapter

        searchDefaultAdapter.callback = callback
        searchAdapter.callback = callback

        loadDefaultRvData()

        viewModel.refreshHotAssets()
    }

    override fun onStop() {
        super.onStop()
        currentSearch?.cancel()
    }

    private fun loadDefaultRvData() = lifecycleScope.launch {
        if (!isAdded) return@launch

        viewModel.observeTopAssets().observe(
            viewLifecycleOwner,
            {
                searchDefaultAdapter.topAssets = it
                if (search_et.text.isNullOrBlank() && rv_va.displayedChild == POS_SEARCH) {
                    rv_va.displayedChild = POS_DEFAULT
                }
            }
        )
        searchDefaultAdapter.recentAssets = loadRecentSearchAssets()
    }

    private suspend fun loadRecentSearchAssets(): List<AssetItem>? {
        return withContext(Dispatchers.IO) {
            val assetList = defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, null)?.split("=")
                ?: return@withContext null
            if (assetList.isNullOrEmpty()) return@withContext null
            val result = viewModel.findAssetsByIds(assetList.take(2))
            if (result.isNullOrEmpty()) return@withContext null
            result.sortedBy {
                assetList.indexOf(it.assetId)
            }
        }
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch = lifecycleScope.launch {
            if (!isAdded) return@launch

            searchAdapter.clear()
            pb.isVisible = true

            val localAssets = viewModel.fuzzySearchAssets(query)
            searchAdapter.localAssets = localAssets

            val pair = viewModel.queryAsset(query)
            val remoteAssets = pair.first
            if (localAssets.isNullOrEmpty()) {
                searchAdapter.remoteAssets = remoteAssets
            } else {
                val filtered = mutableListOf<TopAssetItem>()
                remoteAssets?.forEach { remote ->
                    val exists = localAssets.find { local ->
                        local.assetId == remote.assetId
                    }
                    if (exists == null) {
                        filtered.add(remote)
                    }
                }
                searchAdapter.remoteAssets = filtered
            }
            pb.isVisible = false
        }
    }

    private val callback = object : WalletSearchCallback {
        override fun onAssetClick(assetId: String, assetItem: AssetItem?) {
            search_et?.hideKeyboard()
            if (assetItem != null) {
                view?.navigate(
                    R.id.action_wallet_search_to_transactions,
                    Bundle().apply { putParcelable(ARGS_ASSET, assetItem) }
                )
                viewModel.updateRecentSearchAssets(defaultSharedPreferences, assetId)
            } else {
                lifecycleScope.launch {
                    val dialog = indeterminateProgressDialog(
                        message = R.string.pb_dialog_message,
                    ).apply {
                        setCancelable(false)
                    }
                    dialog.show()
                    val asset = viewModel.findOrSyncAsset(assetId)
                    dialog.dismiss()

                    if (asset == null) return@launch

                    view?.navigate(
                        R.id.action_wallet_search_to_transactions,
                        Bundle().apply { putParcelable(ARGS_ASSET, asset) }
                    )
                    viewModel.updateRecentSearchAssets(defaultSharedPreferences, assetId)
                }
            }
        }
    }
}

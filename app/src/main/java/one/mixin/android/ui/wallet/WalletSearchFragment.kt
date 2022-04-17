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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletSearchBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navigate
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.adapter.SearchAdapter
import one.mixin.android.ui.wallet.adapter.SearchDefaultAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCallback
import one.mixin.android.vo.AssetItem
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WalletSearchFragment : BaseFragment() {
    companion object {
        const val POS_DEFAULT = 0
        const val POS_SEARCH = 1
        const val POS_EMPTY = 2
    }

    private var _binding: FragmentWalletSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel by viewModels<WalletViewModel>()

    private val searchDefaultAdapter by lazy {
        SearchDefaultAdapter()
    }
    private val searchAdapter by lazy {
        SearchAdapter()
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null

    private var currentQuery: String = ""

    private var hasInitializedRootView = false
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        getPersistentView(inflater, container, R.layout.fragment_wallet_search)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            initViews()
        }
    }

    private fun initViews() {
        binding.apply {
            backIb.setOnClickListener {
                searchEt.hideKeyboard()
                activity?.onBackPressed()
            }
            searchEt.setHint(getString(R.string.wallet_search_hint))
            searchEt.post { searchEt.showKeyboard() }
            @SuppressLint("AutoDispose")
            disposable = searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (it.isNullOrBlank()) {
                            rvVa.displayedChild = POS_DEFAULT
                        } else {
                            rvVa.displayedChild = POS_SEARCH
                            if (it.toString() != currentQuery) {
                                currentQuery = it.toString()
                                search(it.toString())
                            }
                        }
                    },
                    {}
                )

            defaultRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            val decoration by lazy { StickyRecyclerHeadersDecoration(searchDefaultAdapter) }
            defaultRv.addItemDecoration(decoration)
            defaultRv.adapter = searchDefaultAdapter

            searchRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            searchRv.adapter = searchAdapter
        }

        searchDefaultAdapter.callback = callback
        searchAdapter.callback = callback

        loadDefaultRvData()

        viewModel.refreshHotAssets()
    }

    override fun onStop() {
        super.onStop()
        currentSearch?.cancel()
        binding.pb.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun loadDefaultRvData() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        viewModel.observeTopAssets().observe(
            viewLifecycleOwner
        ) {
            searchDefaultAdapter.topAssets = it
            if (binding.searchEt.et.text.isNullOrBlank() && binding.rvVa.displayedChild == POS_SEARCH) {
                binding.rvVa.displayedChild = POS_DEFAULT
            }

            checkRecent()
        }
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

    private fun checkRecent() = lifecycleScope.launch {
        if (viewDestroyed()) return@launch

        val recentAssets = searchDefaultAdapter.recentAssets
        if (recentAssets.isNullOrEmpty()) return@launch

        val newRecentList = viewModel.findAssetsByIds(recentAssets.take(2).map { it.assetId })
        var needRefreshRecent = false
        newRecentList.forEach { n ->
            val needUpdate = recentAssets.find { r ->
                r.assetId == n.assetId && r.priceUsd != n.priceUsd
            }
            if (needUpdate != null) {
                needRefreshRecent = true
                return@forEach
            }
        }
        if (needRefreshRecent) {
            searchDefaultAdapter.recentAssets = newRecentList
        }
    }

    private val defaultIconUrl = "https://images.mixin.one/yH_I5b0GiV2zDmvrXRyr3bK5xusjfy5q7FX3lw3mM2Ryx4Dfuj6Xcw8SHNRnDKm7ZVE3_LvpKlLdcLrlFQUBhds=s128"

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch = lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            searchAdapter.submitList(null)
            binding.pb.isVisible = true

            val localAssets = viewModel.fuzzySearchAssets(query)
            searchAdapter.submitList(localAssets)

            val remoteAssets = viewModel.queryAsset(query)
            val result = localAssets?.plus(
                remoteAssets.filterNot { r ->
                    localAssets.any { l ->
                        l.assetId == r.assetId
                    }
                }
            )?.sortedWith(
                Comparator { o1, o2 ->
                    if (o1 == null && o2 == null) return@Comparator 0
                    if (o1 == null) return@Comparator 1
                    if (o2 == null) return@Comparator -1

                    val equal2Keyword1 = o1.symbol.equalsIgnoreCase(query)
                    val equal2Keyword2 = o2.symbol.equalsIgnoreCase(query)
                    if (equal2Keyword1 && !equal2Keyword2) {
                        return@Comparator -1
                    } else if (!equal2Keyword1 && equal2Keyword2) {
                        return@Comparator 1
                    }

                    val capitalization1 = o1.priceFiat() * BigDecimal(o1.balance)
                    val capitalization2 = o2.priceFiat() * BigDecimal(o2.balance)
                    if (capitalization1 != capitalization2) {
                        if (capitalization2 > capitalization1) {
                            return@Comparator 1
                        } else if (capitalization2 < capitalization1) {
                            return@Comparator -1
                        }
                    }

                    val hasIcon1 = o1.iconUrl != defaultIconUrl
                    val hasIcon2 = o2.iconUrl != defaultIconUrl
                    if (hasIcon1 && !hasIcon2) {
                        return@Comparator -1
                    } else if (!hasIcon1 && hasIcon2) {
                        return@Comparator 1
                    }

                    return@Comparator o1.name.compareTo(o2.name)
                }
            )

            searchAdapter.submitList(result)
            binding.pb.isVisible = false

            if (localAssets.isNullOrEmpty() && remoteAssets.isNullOrEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
            }
        }
    }

    private val callback = object : WalletSearchCallback {
        override fun onAssetClick(assetId: String, assetItem: AssetItem?) {
            binding.searchEt.hideKeyboard()
            if (assetItem != null) {
                view?.navigate(
                    R.id.action_wallet_search_to_transactions,
                    Bundle().apply { putParcelable(ARGS_ASSET, assetItem) }
                )
                viewModel.updateRecentSearchAssets(defaultSharedPreferences, assetId)
            } else {
                lifecycleScope.launch {
                    val dialog = indeterminateProgressDialog(
                        message = R.string.Please_wait_a_bit,
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

    private fun getPersistentView(inflater: LayoutInflater?, container: ViewGroup?, @Suppress("SameParameterValue") layout: Int): View {
        if (rootView == null) {
            rootView = inflater?.inflate(layout, container, false)
        } else {
            (rootView?.parent as? ViewGroup)?.removeView(rootView)
        }
        rootView?.let { _binding = FragmentWalletSearchBinding.bind(it) }
        return binding.root
    }
}

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
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletSearchBinding
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.adapter.SearchWeb3Adapter
import one.mixin.android.ui.home.web3.adapter.Web3SearchCallback
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.details.Web3TransactionsFragment
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WalletSearchWeb3Fragment : BaseFragment() {
    companion object {
        const val POS_DEFAULT = 0
        const val POS_SEARCH = 1
        const val POS_EMPTY = 2
        private const val TAG = "WalletSearchWeb3"
    }

    private var _binding: FragmentWalletSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel by viewModels<Web3ViewModel>()
    private val walletViewModel by viewModels<WalletViewModel>()

    private val searchAdapter by lazy {
        SearchWeb3Adapter()
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null

    private var currentQuery: String = ""
    private var isSearchingRemote = false

    private var hasInitializedRootView = false
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        getPersistentView(inflater, container, R.layout.fragment_wallet_search)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            searchEt.setHint(getString(R.string.search_placeholder_asset))
            lifecycleScope.launch {
                delay(200)
                if (isAdded) {
                    searchEt.showKeyboard()
                }
            }
            @SuppressLint("AutoDispose")
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                currentQuery = ""
                                loadAllTokens()
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {},
                    )

            searchRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            searchRv.adapter = searchAdapter
            
            rvVa.displayedChild = POS_DEFAULT
        }

        searchAdapter.callback = callback
        
        loadAllTokens()
    }

    private fun loadAllTokens() {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            
            binding.pb.isVisible = true
            val tokens = withContext(Dispatchers.IO) { 
                val allTokens = viewModel.web3TokensExcludeHidden().value ?: emptyList()
                allTokens.sortedByDescending {
                    runCatching { it.balance.toBigDecimal() * it.priceUsd.toBigDecimal() }.getOrDefault(0.toBigDecimal())
                }
            }
            

            if (tokens.isEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
            } else {
                binding.rvVa.displayedChild = POS_SEARCH
            }
            
            searchAdapter.submitList(tokens)
            binding.pb.isVisible = false
        }
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

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch = lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            binding.pb.isVisible = true
            isSearchingRemote = false

            val localTokens = withContext(Dispatchers.IO) {
                viewModel.web3TokensExcludeHidden().value?.filter { token ->
                    token.name.contains(query, ignoreCase = true) ||
                        token.symbol.contains(query, ignoreCase = true) ||
                        token.chainName?.contains(query, ignoreCase = true) == true
                } ?: emptyList()
            }

            searchAdapter.submitList(localTokens)
            
            if (localTokens.isEmpty()) {
                isSearchingRemote = true
                searchRemote(query)
            } else {
                binding.rvVa.displayedChild = POS_SEARCH
            }
        }
    }
    
    private suspend fun searchRemote(query: String) {
        if (query.length < 2) {
            binding.pb.isVisible = false
            binding.rvVa.displayedChild = POS_EMPTY
            return
        }
        
        try {
            val remoteTokens = withContext(Dispatchers.IO) {
                walletViewModel.queryAsset(query)
            }

            if (remoteTokens.isNotEmpty() && isSearchingRemote) {
                val filteredTokens = remoteTokens.filter {
                    it.chainId in Constants.Web3ChainIds || it.chainId == Constants.ChainId.SOLANA_CHAIN_ID
                }

                val remoteWeb3Tokens = filteredTokens.map { tokenItem ->
                    Web3TokenItem(
                        walletId = "",
                        assetId = tokenItem.assetId,
                        chainId = tokenItem.chainId,
                        name = tokenItem.name,
                        assetKey = tokenItem.assetKey ?: "",
                        symbol = tokenItem.symbol,
                        iconUrl = tokenItem.iconUrl,
                        precision = 0,
                        kernelAssetId = "",
                        balance = "0",
                        priceUsd = tokenItem.priceUsd,
                        changeUsd = tokenItem.changeUsd,
                        chainIcon = tokenItem.chainIconUrl,
                        chainName = tokenItem.chainName,
                        chainSymbol = tokenItem.chainSymbol,
                        hidden = false
                    )
                }
                
                val currentList = searchAdapter.currentList

                val combinedList = currentList + remoteWeb3Tokens.filter { remote ->
                    currentList.none { it.assetId == remote.assetId }
                }

                searchAdapter.submitList(combinedList)
                binding.rvVa.displayedChild = POS_SEARCH
            } else if (isSearchingRemote) {
                binding.rvVa.displayedChild = POS_EMPTY
            }
        } catch (e: Exception) {
            if (isSearchingRemote) {
                binding.rvVa.displayedChild = POS_EMPTY
            }
        } finally {
            binding.pb.isVisible = false
        }
    }

    private val callback =
        object : Web3SearchCallback {
            override fun onTokenClick(token: Web3TokenItem) {
                binding.searchEt.hideKeyboard()
                lifecycleScope.launch {
                    val address = if (token.isSolana()) {
                        PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
                    } else {
                        PropertyHelper.findValueByKey(EVM_ADDRESS, "")
                    }
                    view?.navigate(
                        R.id.action_wallet_search_web3_to_web3_transactions,
                        Bundle().apply {
                            putParcelable("args_token", token)
                            putString("args_address", address)
                        }
                    )
                }
            }
        }

    private fun getPersistentView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        @Suppress("SameParameterValue") layout: Int,
    ): View {
        if (rootView == null) {
            rootView = inflater?.inflate(layout, container, false)
        } else {
            (rootView?.parent as? ViewGroup)?.removeView(rootView)
        }
        rootView?.let { _binding = FragmentWalletSearchBinding.bind(it) }
        return binding.root
    }
}
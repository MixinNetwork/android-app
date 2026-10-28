package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
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
import one.mixin.android.Constants.Account.PREF_WALLET_RECEIVE
import one.mixin.android.Constants.Account.PREF_WALLET_SEND
import one.mixin.android.Constants.Account.PREF_WEB3_WALLET_SEARCH_RECENT
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletSearchBinding
import one.mixin.android.databinding.ItemWalletSearchRecentHeaderBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.addToList
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringList
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.putStringList
import one.mixin.android.extension.navigate
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.adapter.SearchWeb3Adapter
import one.mixin.android.ui.home.web3.adapter.Web3SearchCallback
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WalletSearchWeb3Fragment : BaseFragment() {
    companion object {
        const val POS_DEFAULT = 0
        const val POS_SEARCH = 1
        const val POS_EMPTY = 2
        const val ARGS_WALLET_ID = "args_wallet_id"
        private const val TAG = "WalletSearchWeb3"
    }

    private var _binding: FragmentWalletSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel by viewModels<Web3ViewModel>()
    private val walletViewModel by viewModels<WalletViewModel>()

    private val walletId by lazy { requireArguments().getString(ARGS_WALLET_ID) }

    private val recentHeaderAdapter by lazy { WalletSearchRecentHeaderAdapter { clearRecent() } }
    private val recentAdapter by lazy { SearchWeb3Adapter() }
    private val tokenAdapter by lazy { SearchWeb3Adapter() }

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
            searchRv.adapter = ConcatAdapter(recentHeaderAdapter, recentAdapter, tokenAdapter)

            rvVa.displayedChild = POS_DEFAULT
        }

        val tokenClick = callback
        recentAdapter.callback = tokenClick
        tokenAdapter.callback = tokenClick
        
        loadAllTokens()
    }

    private fun loadAllTokens() {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            
            binding.pb.isVisible = true
            if (walletId.isNullOrEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
                binding.pb.isVisible = false
                return@launch
            }
            val tokens = withContext(Dispatchers.IO) {
                val allTokens = viewModel.web3TokensExcludeHidden(walletId!!).value ?: emptyList()
                allTokens.sortedByDescending {
                    runCatching { it.balance.toBigDecimal() * it.priceUsd.toBigDecimal() }.getOrDefault(0.toBigDecimal())
                }
            }
            

            if (currentQuery.isNotBlank()) return@launch
            if (tokens.isEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
            } else {
                binding.rvVa.displayedChild = POS_SEARCH
            }

            val recent = recentTokens(tokens)
            val recentIds = recent.map { it.assetId }.toSet()
            recentHeaderAdapter.visible = recent.isNotEmpty()
            recentAdapter.submitList(recent)
            tokenAdapter.submitList(tokens.filter { it.assetId !in recentIds })
            binding.pb.isVisible = false
        }
    }

    private fun recentTokens(tokens: List<Web3TokenItem>): List<Web3TokenItem> {
        val walletId = walletId ?: return emptyList()
        val preferences = defaultSharedPreferences
        val key = "$PREF_WEB3_WALLET_SEARCH_RECENT:$walletId"
        val stored = if (preferences.contains(key)) preferences.getStringList(key) else null
        val ids = web3WalletSearchRecentIds(
            storedIds = stored,
            sendIds = preferences.getStringList(PREF_WALLET_SEND).orEmpty(),
            receiveIds = preferences.getStringList(PREF_WALLET_RECEIVE).orEmpty(),
            availableIds = tokens.map { it.assetId }.toSet(),
        )
        if (stored == null && ids.isNotEmpty()) preferences.putStringList(key, ids)
        val tokensById = tokens.associateBy { it.assetId }
        return ids.mapNotNull { tokensById[it] }
    }

    private fun clearRecent() {
        val walletId = walletId ?: return
        defaultSharedPreferences.putStringList("$PREF_WEB3_WALLET_SEARCH_RECENT:$walletId", emptyList())
        if (currentQuery.isBlank()) loadAllTokens()
    }

    override fun onResume() {
        super.onResume()
        if (hasInitializedRootView && currentQuery.isBlank() && _binding != null) loadAllTokens()
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

            if (walletId.isNullOrEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
                binding.pb.isVisible = false
                return@launch
            }

            val localTokens = withContext(Dispatchers.IO) {
                viewModel.web3TokensExcludeHidden(walletId!!).value?.filter { token ->
                    token.name.contains(query, ignoreCase = true) ||
                        token.symbol.contains(query, ignoreCase = true) ||
                        token.chainName?.contains(query, ignoreCase = true) == true
                } ?: emptyList()
            }

            recentHeaderAdapter.visible = false
            recentAdapter.submitList(emptyList())
            tokenAdapter.submitList(localTokens)
            
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
                walletViewModel.queryAsset(walletId, query, true)
            }

            if (remoteTokens.isNotEmpty() && isSearchingRemote) {
                val hasLitecoinAddress = walletId?.let {
                    viewModel.getAddressesByChainId(it, Constants.ChainId.Litecoin)
                } != null
                val filteredTokens = remoteTokens.filter {
                    (it.chainId in Constants.Web3EvmChainIds || it.chainId == Constants.ChainId.SOLANA_CHAIN_ID || it.chainId in Constants.Web3UtxoChainIds) &&
                        (it.chainId != Constants.ChainId.Litecoin || hasLitecoinAddress)
                }
                val remoteWeb3Tokens = filteredTokens.map { tokenItem ->
                    val t = viewModel.web3TokenItemById(walletId!!, tokenItem.assetId)
                    val c = viewModel.web3TokenItemById(walletId!!, tokenItem.chainId)
                    Web3TokenItem(
                        walletId = walletId!!,
                        assetId = tokenItem.assetId,
                        chainId = tokenItem.chainId,
                        name = tokenItem.name,
                        assetKey = tokenItem.assetKey ?: t?.assetKey ?: "",
                        symbol = tokenItem.symbol,
                        iconUrl = tokenItem.iconUrl,
                        precision = t?.precision ?: tokenItem.precision,
                        kernelAssetId = "",
                        balance = t?.balance ?: "0",
                        priceUsd = tokenItem.priceUsd,
                        changeUsd = tokenItem.changeUsd,
                        chainIcon = tokenItem.chainIconUrl ?: t?.chainIcon ?: c?.iconUrl,
                        chainName = tokenItem.chainName ?: c?.name,
                        chainSymbol = tokenItem.chainSymbol ?: c?.chainSymbol,
                        hidden = false,
                        level = tokenItem.level ?: Constants.AssetLevel.UNKNOWN
                    )
                }
                val currentList = tokenAdapter.currentList

                val combinedList = currentList + remoteWeb3Tokens.filter { remote ->
                    currentList.none { it.assetId == remote.assetId }
                }

                tokenAdapter.submitList(combinedList)
                binding.rvVa.displayedChild = POS_SEARCH
            } else if (isSearchingRemote) {
                binding.rvVa.displayedChild = POS_EMPTY
            }
        } catch (e: Exception) {
            if (isSearchingRemote) {
                if (isAdded) {
                    binding.rvVa.displayedChild = POS_EMPTY
                }
            }
        } finally {
            if (isAdded) {
                binding.pb.isVisible = false
            }
        }
    }

    private val callback =
        object : Web3SearchCallback {
            override fun onTokenClick(token: Web3TokenItem) {
                walletId?.let { defaultSharedPreferences.addToList("$PREF_WEB3_WALLET_SEARCH_RECENT:$it", token.assetId) }
                binding.searchEt.hideKeyboard()
                lifecycleScope.launch {
                    val address = walletId?.let {
                        viewModel.getAddressesByChainId(it, token.chainId)
                    } ?: return@launch
                    view?.navigate(
                        R.id.action_wallet_search_web3_to_web3_transactions,
                        Bundle().apply {
                            putParcelable("args_token", token)
                            putString("args_address", address.destination)
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

private class WalletSearchRecentHeaderAdapter(
    private val onClear: () -> Unit,
) : RecyclerView.Adapter<WalletSearchRecentHeaderAdapter.Holder>() {
    var visible: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) notifyItemInserted(0) else notifyItemRemoved(0)
        }

    override fun getItemCount(): Int = if (visible) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemWalletSearchRecentHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false), onClear)

    override fun onBindViewHolder(holder: Holder, position: Int) = Unit

    class Holder(
        binding: ItemWalletSearchRecentHeaderBinding,
        onClear: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.recentClear.setOnClickListener { onClear() }
        }
    }
}

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
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletSearchBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.adapter.SearchWeb3Adapter
import one.mixin.android.ui.home.web3.adapter.Web3SearchCallback
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class WalletSearchWeb3Fragment : BaseFragment() {
    companion object {
        const val POS_SEARCH = 0
        const val POS_EMPTY = 1
    }

    private var _binding: FragmentWalletSearchBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel by viewModels<Web3ViewModel>()

    private val searchAdapter by lazy {
        SearchWeb3Adapter()
    }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null

    private var currentQuery: String = ""

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
        }

        searchAdapter.callback = callback
        
        loadAllTokens()
    }

    private fun loadAllTokens() {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            
            binding.pb.isVisible = true
            val tokens = withContext(Dispatchers.IO) { 
                viewModel.web3Tokens().value?.sortedByDescending { 
                    runCatching { it.balance.toBigDecimal() * it.priceUsd.toBigDecimal() }.getOrDefault(0.toBigDecimal())
                } ?: emptyList()
            }
            
            searchAdapter.submitList(tokens)
            binding.pb.isVisible = false
            
            if (tokens.isEmpty()) {
                binding.rvVa.displayedChild = POS_EMPTY
            } else {
                binding.rvVa.displayedChild = POS_SEARCH
            }
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
        currentSearch =
            lifecycleScope.launch {
                if (viewDestroyed()) return@launch

                searchAdapter.submitList(null)
                binding.pb.isVisible = true

                val localTokens = withContext(Dispatchers.IO) {
                    viewModel.web3Tokens().value?.filter { token ->
                        token.name.contains(query, ignoreCase = true) ||
                            token.symbol.contains(query, ignoreCase = true)
                            // todo
                            // || token.chainName.contains(query, ignoreCase = true)
                    } ?: emptyList()
                }

                searchAdapter.submitList(localTokens)
                binding.pb.isVisible = false

                if (localTokens.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY
                } else {
                    binding.rvVa.displayedChild = POS_SEARCH
                }
            }
    }

    private val callback =
        object : Web3SearchCallback {
            override fun onTokenClick(token: Web3TokenItem) {
                binding.searchEt.hideKeyboard()
                // Todo
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
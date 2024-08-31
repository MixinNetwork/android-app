package one.mixin.android.web3.dapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchDappsBinding
import one.mixin.android.extension.showKeyboard
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.exploreEvm
import one.mixin.android.ui.home.exploreSolana
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.viewBinding
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchDappFragment : BaseFragment(R.layout.fragment_search_dapps) {
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private val searchAdapter: SearchDappAdapter by lazy {
        SearchDappAdapter { url ->
            WebActivity.show(requireContext(), url, null)
        }
    }

    private var chainId: String = Chain.Ethereum.chainId

    companion object {
        const val TAG = "SearchDappFragment"
        const val SEARCH_DEBOUNCE = 300L
    }

    private var keyword: String? = null
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    private fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            searchAdapter.query = text
            lifecycleScope.launch {
                searchAdapter.url = web3ViewModel.fuzzySearchUrl(text)
            }
            keyword = text
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindData(keyword: String? = this@SearchDappFragment.keyword) {
        fuzzySearch(keyword)
    }

    private val binding by viewBinding(FragmentSearchDappsBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        chainId = if (exploreSolana(requireContext())) {
            Chain.Solana.chainId
        } else {
            Chain.Ethereum.chainId
        }
        view.setOnClickListener {
            if (keyword.isNullOrBlank()) {
                (requireActivity() as MainActivity).closeSearch()
            }
        }
        binding.radioEth.isChecked = exploreEvm(requireContext())
        binding.radioSolana.isChecked = exploreSolana(requireContext())
        binding.radioNetwork.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_eth -> {
                    lifecycleScope.launch {
                        chainId = Chain.Ethereum.chainId
                        fuzzySearch(binding.searchEt.text.toString())
                    }
                }

                R.id.radio_solana -> {
                    lifecycleScope.launch {
                        chainId = Chain.Solana.chainId
                        fuzzySearch(binding.searchEt.text.toString())
                    }
                }

                else -> {}
            }
        }
        binding.searchEt.setHint(R.string.search_placeholder_dapp)
        binding.searchRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.searchRv.adapter = searchAdapter
        binding.backIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        lifecycleScope.launch {
            delay(200)
            if (isAdded) {
                binding.searchEt.showKeyboard()
            }
        }

        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe(
                {
                    setQueryText(it.toString())
                },
                {},
            )
        lifecycleScope.launch {
            fuzzySearch(null)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fuzzySearch(keyword: String?) {
        lifecycleScope.launch {
            if (keyword.isNullOrBlank()) {
                binding.searchRv.isVisible = true
                binding.empty.isVisible = false
                searchAdapter.userList = web3ViewModel.dapps(chainId)
                searchAdapter.notifyDataSetChanged()
            } else {
                searchAdapter.clear()
                val dappList =
                    web3ViewModel.dapps(chainId).filter { dapp ->
                        dapp.name.contains(keyword) || dapp.homeUrl.contains(keyword)
                    }
                searchAdapter.userList = dappList
                if (dappList.isEmpty()) {
                    binding.searchRv.isVisible = false
                    binding.empty.isVisible = true
                } else {
                    binding.searchRv.isVisible = true
                    binding.empty.isVisible = false
                }
                searchAdapter.notifyDataSetChanged()
            }
        }
    }
}

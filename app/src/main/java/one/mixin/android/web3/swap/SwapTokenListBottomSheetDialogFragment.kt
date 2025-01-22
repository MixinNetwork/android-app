package one.mixin.android.web3.swap

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.BinanceSmartChain
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.Polygon
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.ChainId.TRON_CHAIN_ID
import one.mixin.android.Constants.ChainId.Base
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.sortByKeywordAndBalance
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapViewModel
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.web3.swap.Components.RecentTokens
import one.mixin.android.widget.BottomSheet
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SwapTokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val ARGS_KEY = "args_key"
        const val ARGS_UNIQUE = "args_unique"
        const val TAG = "SwapTokenListBottomSheetDialogFragment"

        fun newInstance(key: String, tokens: ArrayList<SwapToken>, selectUnique: String? = null) =
            SwapTokenListBottomSheetDialogFragment().withArgs {
                putString(ARGS_KEY, key)
                putString(ARGS_UNIQUE, selectUnique)
            }.also { fragment ->
                fragment.setTokens(tokens)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)
    private val swapViewModel by viewModels<SwapViewModel>()

    private var tokens: List<SwapToken> = emptyList()

    private val key by lazy {
        requireNotNull(requireArguments().getString(ARGS_KEY))
    }

    private val selectUnique by lazy {
        requireArguments().getString(ARGS_UNIQUE)
    }

    private val adapter by lazy {
        SwapTokenAdapter(selectUnique)
    }

    private var isLoading = false

    fun setTokens(newTokens: List<SwapToken>) {
        tokens = newTokens
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLoading(loading: Boolean, list: List<SwapToken>? = null) {
        if (isLoading == loading) return
        isLoading = loading
        if (list != null) {
            tokens = list
            binding.radio.isVisible = !isLoading
            initRadio()
            filter(binding.searchEt.et.text?.toString() ?: "")
        }
    }

    private fun initRadio() {
        binding.apply {
            if (!inMixin()) { // only solana network
                radioSolana.isChecked = true
                radioAll.isVisible = false
                radioEth.isVisible = false
                radioBase.isVisible = false
                radioTron.isVisible = false
                radioBsc.isVisible = false
                radioPolygon.isVisible = false
            } else {
                radioAll.isChecked = true
                radioAll.isVisible = true
                radioEth.isVisible = true
                radioBase.isVisible = true
                radioTron.isVisible = true
                radioBsc.isVisible = true
                radioPolygon.isVisible = true
                radioGroup.setOnCheckedChangeListener { _, id ->
                    currentChain = when (id) {
                        R.id.radio_eth -> {
                            ETHEREUM_CHAIN_ID
                        }

                        R.id.radio_solana -> {
                            SOLANA_CHAIN_ID
                        }

                        R.id.radio_base -> {
                            Base
                        }

                        R.id.radio_tron -> {
                            TRON_CHAIN_ID
                        }

                        R.id.radio_bsc -> {
                            BinanceSmartChain
                        }

                        R.id.radio_polygon -> {
                            Polygon
                        }

                        else -> {
                            null
                        }
                    }
                    filter(searchEt.et.text?.toString() ?: "")
                }
            }
        }
    }

    private fun Dialog.setViewTreeOwners() {
        val decorView = window?.decorView ?: return
        decorView.setViewTreeLifecycleOwner(this@SwapTokenListBottomSheetDialogFragment)
        decorView.setViewTreeViewModelStoreOwner(this@SwapTokenListBottomSheetDialogFragment)
        decorView.setViewTreeSavedStateRegistryOwner(this@SwapTokenListBottomSheetDialogFragment)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        dialog.setViewTreeOwners()
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight() + requireContext().appCompatActionBarHeight()
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            assetRv.adapter = adapter
            adapter.tokens = tokens
            radio.isVisible = !isLoading
            initRadio()
            searchEt.et.setHint(if (inMixin()) R.string.search_placeholder_asset else R.string.search_swap_token)
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            if (isLoading) {
                rvVa.displayedChild = 3
            } else if (tokens.isEmpty()) {
                rvVa.displayedChild = 2
            } else {
                rvVa.displayedChild = 0
            }
            depositTv.setText(R.string.Receive)
            depositTv.setOnClickListener {
                onDepositListener?.invoke()
            }
            searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(destroyScope)
                .subscribe({
                    searchJob?.cancel()
                    searchJob = filter(it.toString())
                }, {})

        }
    }

    private val composeId by lazy {
        View.generateViewId()
    }

    override fun onStart() {
        super.onStart()
        binding.apply {
            root.findViewById<ComposeView>(composeId).let {
                if (it == null) {
                    val composeView = ComposeView(requireContext()).apply {
                        id = View.generateViewId()
                        setContent {
                            RecentTokens(key) {
                                AnalyticsTracker.trackSwapCoinSwitch(AnalyticsTracker.SwapCoinSwitchMethod.RECENT_CLICK)
                                adapter.onClick(it)
                            }
                        }
                    }

                    root.addView(
                        composeView,
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.BELOW, searchView.id)
                        })

                    radio.updateLayoutParams<RelativeLayout.LayoutParams> {
                        addRule(RelativeLayout.BELOW, composeView.id)
                    }
                    root.requestLayout()
                    root.invalidate()
                }
            }
        }
    }

    private var searchJob: Job? = null

    private var currentChain: String? = null
        set(value) {
            field = value
            adapter.all = currentChain == null
        }

    private fun filter(s: String) =
        lifecycleScope.launch {
            if (s.isBlank() && currentChain == null) {
                adapter.tokens = tokens
                adapter.isSearch = false
                if (isLoading) {
                    binding.rvVa.displayedChild = 3
                } else if (tokens.isEmpty()) {
                    binding.rvVa.displayedChild = 2
                } else {
                    binding.rvVa.displayedChild = 0
                }
                return@launch
            }
            val assetList =
                tokens.filter {
                    (currentChain != null && it.chain.chainId == currentChain) || currentChain == null
                }.toMutableList()

            val total = search(s, assetList, currentChain, inMixin())
            adapter.tokens = ArrayList(total.sortByKeywordAndBalance(s))
            adapter.isSearch = true
            if (!isAdded) {
                return@launch
            }
            if (isLoading) {
                binding.rvVa.displayedChild = 3
            } else if (adapter.itemCount == 0) {
                binding.rvVa.displayedChild = 1
            } else {
                binding.rvVa.displayedChild = 0
            }
            binding.pb.isVisible = false
        }

    private suspend fun search(
        s: String,
        localTokens: MutableList<SwapToken>,
        currentChain: String?,
        inMixin: Boolean,
    ): List<SwapToken> {
        if (s.isBlank()) return localTokens
        binding.pb.isVisible = true
        val remoteList = handleMixinResponse(
            invokeNetwork = { swapViewModel.searchTokens(s, inMixin) },
            successBlock = { resp ->
                return@handleMixinResponse resp.data?.filter { currentChain == null || (it.chain.chainId == currentChain) }?.map { token ->
                    if (inMixin) {
                        token.copy(address = "")
                    } else {
                        token.copy(assetId = "")
                    }
                }?.map { ra ->
                    localTokens.find { swapToken -> swapToken.getUnique() == ra.getUnique() }?.let {
                        return@map ra.copy(price = it.price, balance = it.balance, collectionHash = it.collectionHash)
                    }
                    return@map ra
                }
            },
            endBlock = {
                binding.pb.isVisible = false
            }
        )
        return remoteList ?: emptyList()
    }

    fun setOnClickListener(onClickListener: (SwapToken, Boolean) -> Unit) {
        this.adapter.setOnClickListener(onClickListener)
    }

    fun setOnDeposit(onDepositListener: () -> Unit) {
        this.onDepositListener = onDepositListener
    }

    private var onDepositListener: (() -> Unit)? = null

    private fun inMixin(): Boolean = key == Constants.Account.PREF_TO_SWAP || key == Constants.Account.PREF_FROM_SWAP
}

package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.Arbitrum
import one.mixin.android.Constants.ChainId.Avalanche
import one.mixin.android.Constants.ChainId.Base
import one.mixin.android.Constants.ChainId.BinanceSmartChain
import one.mixin.android.Constants.ChainId.ETHEREUM_CHAIN_ID
import one.mixin.android.Constants.ChainId.Optimism
import one.mixin.android.Constants.ChainId.Polygon
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.Constants.ChainId.TON_CHAIN_ID
import one.mixin.android.Constants.ChainId.TRON_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentAssetListBottomSheetBinding
import one.mixin.android.extension.addToList
import one.mixin.android.extension.appCompatActionBarHeight
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.scrollToCenterCheckedRadio
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.adapter.SearchAdapter
import one.mixin.android.ui.wallet.adapter.WalletSearchCallback
import one.mixin.android.ui.wallet.components.RecentTokens
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class TokenListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TokenListBottomSheetDialogFragment"
        const val ARGS_FOR_TYPE = "args_for_type"
        const val ARGS_ASSET_ID = "args_asset_id"

        const val POS_RV = 0
        const val POS_EMPTY_RECEIVE = 1

        const val TYPE_FROM_SEND = 0
        const val TYPE_FROM_RECEIVE = 1
        const val TYPE_FROM_TRANSFER = 2

        const val ASSET_PREFERENCE = "TRANSFER_ASSET"

        fun newInstance(
            fromType: Int,
            currentAssetId: String? = null,
        ) =
            TokenListBottomSheetDialogFragment().withArgs {
                putInt(ARGS_FOR_TYPE, fromType)
                putString(ARGS_ASSET_ID, currentAssetId)
            }
    }

    private val binding by viewBinding(FragmentAssetListBottomSheetBinding::inflate)

    private val fromType: Int by lazy {
        requireArguments().getInt(ARGS_FOR_TYPE)
    }

    private val key by lazy {
        when (fromType) {
            TYPE_FROM_SEND, TYPE_FROM_TRANSFER -> Constants.Account.PREF_WALLET_SEND
            TYPE_FROM_RECEIVE -> Constants.Account.PREF_WALLET_RECEIVE
            else -> Constants.Account.PREF_WALLET_SEND
        }
    }

    private val adapter by lazy { SearchAdapter(requireArguments().getString(ARGS_ASSET_ID)) }

    private var disposable: Disposable? = null
    private var currentSearch: Job? = null
    private var currentQuery: String = ""
    private var defaultAssets = emptyList<TokenItem>()
    private var currentChain: String? = null

    private fun initRadio() {
        binding.apply {
            radio.isVisible = true
            radioAll.isChecked = true
            radio.scrollToCenterCheckedRadio(radioGroup)
            radioAll.isVisible = true
            radioEth.isVisible = true
            radioTron.isVisible = true
            radioBase.isVisible = true
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

                    R.id.radio_arbritrum -> {
                        Arbitrum
                    }

                    R.id.radio_optimism -> {
                        Optimism
                    }

                    R.id.radio_toncoin -> {
                        TON_CHAIN_ID
                    }

                    R.id.radio_avalanche -> {
                        Avalanche
                    }

                    else -> {
                        null
                    }
                }
                radio.scrollToCenterCheckedRadio(radioGroup)
                loadData()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        binding.ph.doOnPreDraw {
            binding.ph.updateLayoutParams<ViewGroup.LayoutParams> {
                height = binding.ph.getSafeAreaInsetsTop() + requireContext().appCompatActionBarHeight()
            }
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }

        binding.apply {
            initRadio()
            closeIb.setOnClickListener {
                searchEt.hideKeyboard()
                dismiss()
            }
            assetRv.adapter = adapter
            adapter.callback =
                object : WalletSearchCallback {
                    override fun onAssetClick(
                        assetId: String,
                        tokenItem: TokenItem?,
                    ) {
                        binding.searchEt.hideKeyboard()
                        tokenItem?.let {
                            if (asyncOnAsset != null) {
                                asyncClick(it)
                            } else {
                                defaultSharedPreferences.addToList(key, it.assetId)
                                onAsset?.invoke(it)
                            }
                        }
                        dismiss()
                    }
                }
            searchEt.setHint(getString(R.string.search_placeholder_asset))

            @SuppressLint("AutoDispose")
            disposable =
                searchEt.et.textChanges().debounce(500L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe(
                        {
                            if (it.isNullOrBlank()) {
                                binding.rvVa.displayedChild = POS_RV
                                adapter.submitList(defaultAssets)
                            } else {
                                if (it.toString() != currentQuery) {
                                    currentQuery = it.toString()
                                    search(it.toString())
                                }
                            }
                        },
                        {},
                    )
        }

        if (fromType == TYPE_FROM_SEND || fromType == TYPE_FROM_TRANSFER) {
            bottomViewModel.assetItemsWithBalance()
        } else {
            bottomViewModel.assetItemsNotHidden()
        }.observe(this) {
            defaultAssets = it
            if (fromType == TYPE_FROM_SEND) {
                adapter.submitList(defaultAssets)
                if (defaultAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
                } else {
                    binding.rvVa.displayedChild = POS_RV
                }
            } else {
                if (binding.searchEt.et.text.isNullOrBlank()) {
                    adapter.submitList(defaultAssets)
                }
            }
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
                            RecentTokens (false, key) {
                                defaultSharedPreferences.addToList(key, it.assetId)
                                if (asyncOnAsset != null) {
                                    asyncClick(it)
                                } else {
                                    this@TokenListBottomSheetDialogFragment.onAsset?.invoke(it)
                                    dismiss()
                                }
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

    private fun loadData() {
        adapter.chain = currentChain
        binding.rvVa.displayedChild = when (adapter.getFilteredTokens().size) {
            0 -> POS_EMPTY_RECEIVE
            else -> POS_RV
        }
        binding.assetRv.scrollToPosition(0)
        binding.pb.isVisible = false
    }

    private fun search(query: String) {
        currentSearch?.cancel()
        currentSearch =
            lifecycleScope.launch {
                if (!isAdded) return@launch

                binding.rvVa.displayedChild = POS_RV
                binding.pb.isVisible = true

                val localAssets = bottomViewModel.fuzzySearchAssets(query)?.filter {
                    if (TYPE_FROM_SEND == fromType) {
                        it.balance.toBigDecimalOrNull().run { this != null && this > BigDecimal.ZERO }
                    } else {
                        true
                    }
                }
                adapter.submitList(localAssets)
                val remoteAssets = if (TYPE_FROM_SEND == fromType) emptyList() else
                    bottomViewModel.queryAsset(walletId = null, query = query).map {
                        val local = bottomViewModel.findAssetItemById(it.assetId)
                        if (local != null) {
                            it.copy(balance = local.balance)
                        } else {
                            it
                        }
                    }
                val result = sortQueryAsset(query, localAssets, remoteAssets)

                adapter.submitList(result) {
                    binding.assetRv.scrollToPosition(0)
                }
                binding.pb.isVisible = false

                if (localAssets.isNullOrEmpty() && remoteAssets.isEmpty()) {
                    binding.rvVa.displayedChild = POS_EMPTY_RECEIVE
                }
                
                if (!isAdded) return@launch
                loadData()
            }
    }

    fun setOnAssetClick(callback: (TokenItem) -> Unit): TokenListBottomSheetDialogFragment {
        this.onAsset = callback
        return this
    }

    fun setOnDepositClick(callback: () -> Unit): TokenListBottomSheetDialogFragment {
        this.onDeposit = callback
        return this
    }

    private fun asyncClick(token: TokenItem) {
        lifecycleScope.launch {
            val dialog =
                indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                    setCancelable(false)
                }
            asyncOnAsset?.invoke(token)
            dialog.dismiss()
            dismiss()
        }
    }

    private var onAsset: ((TokenItem) -> Unit)? = null
    private var onDeposit: (() -> Unit)? = null

    var asyncOnAsset: (suspend (TokenItem) -> Unit)? = null
}

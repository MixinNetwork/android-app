package one.mixin.android.web3.details

import android.content.ClipData
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.isSolToken
import one.mixin.android.db.web3.vo.solLamportToAmount
import one.mixin.android.databinding.FragmentWeb3TransactionDetailsBinding
import one.mixin.android.databinding.ViewWalletWeb3TokenBottomBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navTo
import one.mixin.android.extension.navigate
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.setQuoteText
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPriceJob
import one.mixin.android.job.RefreshWeb3TransactionJob
import one.mixin.android.tip.Tip
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.home.web3.stake.StakeFragment
import one.mixin.android.ui.home.web3.stake.ValidatorsFragment
import one.mixin.android.ui.home.web3.swap.SwapFragment
import one.mixin.android.ui.wallet.AllTransactionsFragment
import one.mixin.android.ui.wallet.AllWeb3TransactionsFragment
import one.mixin.android.ui.wallet.adapter.OnSnapshotListener
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.web3.details.Web3TransactionFragment.Companion.ARGS_CHAIN
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.DebugClickListener
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class Web3TransactionDetailsFragment : BaseFragment(R.layout.fragment_web3_transaction_details), OnSnapshotListener {
    companion object {
        const val TAG = "Web3TransactionDetailsFragment"
        const val ARGS_TOKEN = "args_token"
        const val ARGS_TOKENS = "args_tokens"
        const val ARGS_CHAIN_TOKEN = "args_chain_token"
        const val ARGS_ADDRESS = "args_address"

        fun newInstance(
            address: String,
            chain: String,
            web3Token: Web3TokenItem,
            tokens: List<Web3TokenItem>? = null
        ) =
            Web3TransactionDetailsFragment().withArgs {
                putString(ARGS_ADDRESS, address)
                putString(ARGS_CHAIN, chain)
                putParcelable(ARGS_TOKEN, web3Token)
                putParcelableArrayList(ARGS_TOKENS, arrayListOf<Web3TokenItem>().apply {
                    add(web3Token)
                    tokens?.let {
                        addAll(tokens.filter { it != web3Token })
                    }
                })
            }
    }

    private val binding by viewBinding(FragmentWeb3TransactionDetailsBinding::bind)
    private val web3ViewModel by viewModels<Web3ViewModel>()

    private var _bottomBinding: ViewWalletWeb3TokenBottomBinding? = null
    private val bottomBinding get() = requireNotNull(_bottomBinding) { "required _bottomBinding is null" }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var tip: Tip

    private val address: String by lazy {
        requireNotNull(requireArguments().getString(ARGS_ADDRESS))
    }

    private val chain: String by lazy {
        requireNotNull(requireArguments().getString(ARGS_CHAIN))
    }

    private val web3tokens by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_TOKENS, Web3TokenItem::class.java)!!
    }

    private val token: Web3TokenItem by lazy {
        requireArguments().getParcelableCompat(ARGS_TOKEN, Web3TokenItem::class.java)!!
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(RefreshPriceJob(token.assetId))
        binding.titleView.apply {
            titleTv.setTextOnly(token.name)
            leftIb.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            rightIb.setOnClickListener {
                val builder = BottomSheet.Builder(requireActivity())
                _bottomBinding = ViewWalletWeb3TokenBottomBinding.bind(
                    View.inflate(
                        ContextThemeWrapper(
                            requireActivity(),
                            R.style.Custom
                        ), R.layout.view_wallet_web3_token_bottom, null
                    )
                )
                builder.setCustomView(bottomBinding.root)
                val bottomSheet = builder.create()
                bottomBinding.apply {
                    title.text = token.name
                    addressTv.text = token.assetKey
                    explorer.setOnClickListener {
                        if (token.isSolana()) {
                            context?.openUrl("https://solscan.io/token/" + token.assetKey)
                        } else {
                            // TODO more evm
                            context?.openUrl("https://etherscan.io/token/" + token.assetKey)
                        }
                        bottomSheet.dismiss()
                    }
                    stakeSolTv.isVisible = token.isSolToken()
                    stakeSolTv.setOnClickListener {
                        this@Web3TransactionDetailsFragment.navTo(
                            ValidatorsFragment.newInstance().apply {
                                setOnSelect { v ->
                                    this@Web3TransactionDetailsFragment.navTo(
                                        StakeFragment.newInstance(
                                            v,
                                            token.balance
                                        ), StakeFragment.TAG
                                    )
                                }
                            }, ValidatorsFragment.TAG
                        )
                        bottomSheet.dismiss()
                    }
                    copy.setOnClickListener {
                        context?.getClipboardManager()
                            ?.setPrimaryClip(ClipData.newPlainText(null, token.assetKey))
                        toast(R.string.copied_to_clipboard)
                        bottomSheet.dismiss()
                    }
                    cancel.setOnClickListener { bottomSheet.dismiss() }
                }

                bottomSheet.show()
            }

            binding.apply {
                web3ViewModel.marketById(token.assetId).observe(viewLifecycleOwner) { market ->
                    if (market != null) {
                        val priceChangePercentage24H = BigDecimal(market.priceChangePercentage24H)
                        val isRising = priceChangePercentage24H >= BigDecimal.ZERO
                        rise.setQuoteText(
                            "${(priceChangePercentage24H).numberFormat2()}%",
                            isRising
                        )
                    } else if (token.priceUsd == "0") {
                        rise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        rise.text = "0.00%"
                    } else if (token.changeUsd.isNotEmpty()) {
                        val changeUsd = BigDecimal(token.changeUsd)
                        val isRising = changeUsd >= BigDecimal.ZERO
                        rise.setQuoteText(
                            "${(changeUsd * BigDecimal(100)).numberFormat2()}%",
                            isRising
                        )
                    }
                }
                if (token.isSolToken()) {
                    stake.root.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        getStakeAccounts(address)
                    }
                }
                transactionsRv.listener = this@Web3TransactionDetailsFragment
                bottomCard.post {
                    bottomCard.isVisible = true
                    val remainingHeight =
                        requireContext().screenHeight() - requireContext().statusBarHeight() - requireContext().navigationBarHeight() - titleView.height - topLl.height - marketRl.height - 70.dp
                    bottomRl.updateLayoutParams {
                        height = remainingHeight
                    }

                    if (scrollY > 0) {
                        scrollView.isInvisible = true
                        scrollView.postDelayed(
                            {
                                scrollView.scrollTo(0, scrollY)
                                scrollView.isInvisible = false
                            }, 1
                        )
                    }
                }

                sendReceiveView.send.setOnClickListener {
                    lifecycleScope.launch {
                        val chain = web3ViewModel.web3TokenItemByChainId(token.chainId)
                        navTo(TransferDestinationInputFragment.newInstance(address, token, chain), TransferDestinationInputFragment.TAG)
                    }
                }
                sendReceiveView.receive.setOnClickListener {
                    navTo(Web3AddressFragment(), Web3AddressFragment.TAG)
                }
                sendReceiveView.swap.isVisible = token.isSolana()
                sendReceiveView.swap.setOnClickListener {
                    AnalyticsTracker.trackSwapStart("solana", "solana")
                    navTo(SwapFragment.newInstance<Web3TokenItem>(web3tokens), SwapFragment.TAG)
                }
                transactionsTitleLl.setOnClickListener {
                    navTo(AllWeb3TransactionsFragment.newInstance(tokenItem = token), AllWeb3TransactionsFragment.TAG, AllWeb3TransactionsFragment.TAG)
                }
                marketView.setContent {
                    Market(token.assetId)
                }
            }
        }

        web3ViewModel.web3Transactions(token.assetId).observe(viewLifecycleOwner) { list->
            binding.transactionsRv.isVisible = list.isNotEmpty()
            binding.bottomRl.isVisible = list.isEmpty()
            binding.transactionsRv.list = list
        }

        updateHeader(token) //todo Live data
    }


    private fun updateHeader(asset: Web3TokenItem) {
        binding.apply {
            val amountText =
                try {
                    if (asset.balance.toFloat() == 0f) {
                        "0.00"
                    } else {
                        asset.balance.numberFormat()
                    }
                } catch (ignored: NumberFormatException) {
                    asset.balance.numberFormat()
                }
            val color = requireContext().colorFromAttribute(R.attr.text_primary)
            balance.text = buildAmountSymbol(requireContext(), amountText, asset.symbol, color, color)
            balanceAs.text =
                try {
                    if (asset.fiat().toFloat() == 0f) {
                        "≈ ${Fiats.getSymbol()}0.00"
                    } else {
                        "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                    }
                } catch (ignored: NumberFormatException) {
                    "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                }
            avatar.loadToken(asset)
            avatar.setOnClickListener(
                object : DebugClickListener() {
                    override fun onDebugClick() {
                    }

                    override fun onSingleClick() {
                    }
                },
            )
        }
    }

    private suspend fun getStakeAccounts(address: String) {
        val stakeAccounts = web3ViewModel.getStakeAccounts(address)
        if (stakeAccounts.isNullOrEmpty()) {
            updateStake(StakeAccountSummary(0, "0"))
            return
        }

        var amount: Long = 0
        var count = 0
        stakeAccounts.forEach { a ->
            count++
            amount += (a.account.data.parsed.info.stake.delegation.stake.toLongOrNull() ?: 0)
        }

        val stakeAccountSummary = StakeAccountSummary(count, amount.solLamportToAmount().stripTrailingZeros().toPlainString())
        updateStake(stakeAccountSummary)
    }

    private fun updateStake(stakeAccountSummary: StakeAccountSummary?) {
        binding.stake.apply {
            if (stakeAccountSummary == null) {
                iconVa.displayedChild = 0
                amountTv.text = "0 SOL"
                countTv.text = "0 account"
            } else {
                iconVa.displayedChild = 1
                amountTv.text = "${stakeAccountSummary.amount} SOL"
                countTv.text = "${stakeAccountSummary.count} account"
                stakeRl.setOnClickListener {
                    // Todo
                    // onClickAction?.invoke(stake.stakeRl.id)
                }
            }
        }
    }

    override fun <T> onNormalItemClick(item: T) {
        item as Web3TransactionItem
        navTo(
            Web3TransactionFragment.newInstance(item, chain, token),
            Web3TransactionFragment.TAG
        )
    }

    override fun onUserClick(userId: String) {
        // Do nothing
    }

    override fun onMoreClick() {
        navTo(AllWeb3TransactionsFragment.newInstance(tokenItem = token), AllWeb3TransactionsFragment.TAG, AllWeb3TransactionsFragment.TAG)
    }
}

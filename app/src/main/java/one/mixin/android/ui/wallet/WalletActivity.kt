package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getSerializableExtraCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_FROM_MARKET
import one.mixin.android.ui.wallet.fiatmoney.CalculateFragment
import one.mixin.android.ui.wallet.fiatmoney.FiatMoneyViewModel
import one.mixin.android.ui.wallet.fiatmoney.RouteProfile
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class WalletActivity : BlazeBaseActivity() {
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        val account = Session.getAccount()
        if (account == null) {
            finish()
            return
        }

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.container) as? NavHostFragment?
        val navController = navHostFragment?.navController ?: return
        val navGraph = navController.navInflater.inflate(R.navigation.nav_wallet)

        val isBuy = intent.extras?.getBoolean(BUY)
        if (isBuy == true) {
            navGraph.setStartDestination(R.id.wallet_calculate)
            navController.setGraph(navGraph, null)
            return
        }

        val destination = requireNotNull(intent.getSerializableExtraCompat(DESTINATION, Destination::class.java)) { "required destination can not be null" }
        when (destination) {
            Destination.Transactions -> {
                navGraph.setStartDestination(R.id.transactions_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                val fromMarket = intent.getBooleanExtra(ARGS_FROM_MARKET, false)
                navController.setGraph(navGraph, Bundle().apply {
                    putParcelable(ARGS_ASSET, token)
                    putBoolean(ARGS_FROM_MARKET, fromMarket)
                })
            }
            Destination.Search -> {
                navGraph.setStartDestination(R.id.wallet_search_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.SearchWeb3 -> {
                navGraph.setStartDestination(R.id.wallet_search_web3_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.AllTransactions -> {
                navGraph.setStartDestination(R.id.all_transactions_fragment)
                val pendingType = intent.getBooleanExtra(PENDING_TYPE, false)
                navController.setGraph(navGraph, Bundle().apply {
                    putBoolean(PENDING_TYPE, pendingType)
                })
            }
            Destination.AllWeb3Transactions -> {
                navGraph.setStartDestination(R.id.all_web3_transactions_fragment)
                val web3FilterParams = intent.getParcelableExtraCompat(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    web3FilterParams?.let { putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, it) }
                })
            }
            Destination.Hidden -> {
                navGraph.setStartDestination(R.id.hidden_assets_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.Web3Hidden -> {
                navGraph.setStartDestination(R.id.web3_hidden_assets_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.Deposit -> {
                navGraph.setStartDestination(R.id.deposit_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            Destination.Buy -> {
                navGraph.setStartDestination(R.id.wallet_calculate)
                val state = intent.getParcelableExtraCompat(CalculateFragment.CALCULATE_STATE, FiatMoneyViewModel.CalculateState::class.java)
                routeProfile = intent.getParcelableExtraCompat(ARGS_ROUTE_PROFILE, RouteProfile::class.java)
                navController.setGraph(navGraph, Bundle().apply { state?.let { s -> putParcelable(CalculateFragment.CALCULATE_STATE, s) } })
            }
            Destination.Market -> {
                navGraph.setStartDestination(R.id.market_fragment_details)
                val marketItem = intent.getParcelableExtraCompat(ARGS_MARKET, MarketItem::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    marketItem?.let {
                        putParcelable(ARGS_MARKET, it)
                    }
                })
            }
            Destination.Address -> {
                navGraph.setStartDestination(R.id.web3_address_fragment)
                val address = requireNotNull(intent.getStringExtra(ADDRESS)) { "required address can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putString(ADDRESS, address) })
            }
            Destination.Web3Transactions -> {
                navGraph.setStartDestination(R.id.web3_transactions_fragment)
                val web3Token = requireNotNull(intent.getParcelableExtraCompat(WEB3_TOKEN, Web3TokenItem::class.java)) { "required web3 token can not be null" }
                val address = requireNotNull(intent.getStringExtra(ADDRESS)) { "required address can not be null" }
                navController.setGraph(navGraph, Bundle().apply {
                    putParcelable("args_token", web3Token)
                    putString("args_address", address)
                })
            }
            Destination.Web3TransferDestinationInput -> {
                navGraph.setStartDestination(R.id.transfer_destination_input_fragment)
                val address = intent.getStringExtra(TransferDestinationInputFragment.ARGS_ADDRESS)
                val token = intent.getParcelableExtraCompat(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
                val chain = intent.getParcelableExtraCompat(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, Web3TokenItem::class.java)
                val asset = intent.getParcelableExtraCompat(TransactionsFragment.ARGS_ASSET, TokenItem::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    asset?.let { asset-> putParcelable(TransactionsFragment.ARGS_ASSET, asset) }
                    address?.let { address -> putString(TransferDestinationInputFragment.ARGS_ADDRESS, address) }
                    token?.let { token -> putParcelable(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token) }
                    chain?.let { chain -> putParcelable(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chain) }
                })
            }
        }
    }

    var routeProfile: RouteProfile? = null

    enum class Destination {
        Transactions,
        Search,
        SearchWeb3,
        AllTransactions,
        AllWeb3Transactions,
        Hidden,
        Web3Hidden,
        Deposit,
        Buy,
        Market,
        Address,
        Web3Transactions,
        Web3TransferDestinationInput,
    }

    companion object {
        const val DESTINATION = "destination"
        const val ASSET = "ASSET"
        const val BUY = "buy"
        const val ARGS_ROUTE_PROFILE = "args_route_profile"
        const val ADDRESS = "address"
        const val WEB3_TOKEN = "web3_token"
        const val PENDING_TYPE = "pending_type"

        fun navigateToWalletActivity(activity: Activity, address: String, token: Web3TokenItem, chain: Web3TokenItem) {
            val intent = Intent(activity, WalletActivity::class.java).apply {
                putExtra(TransferDestinationInputFragment.ARGS_ADDRESS, address)
                putExtra(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token)
                putExtra(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chain)
                putExtra(DESTINATION, Destination.Web3TransferDestinationInput)
            }
            activity.startActivity(intent)
        }

        fun navigateToWalletActivity(activity: Activity, asset: TokenItem) {
            val intent = Intent(activity, WalletActivity::class.java).apply {
                putExtra(TransactionsFragment.ARGS_ASSET, asset)
                putExtra(DESTINATION, Destination.Web3TransferDestinationInput)
            }
            activity.startActivity(intent)
        }

        fun showWithToken(
            activity: Activity,
            tokenItem: TokenItem,
            destination: Destination,
            fromMarket: Boolean = false
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(ASSET, tokenItem)
                    putExtra(ARGS_FROM_MARKET, fromMarket)
                },
            )
        }

        fun showBuy(
            activity: Activity,
            state: FiatMoneyViewModel.CalculateState?,
            routeProfile: RouteProfile?,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Buy)
                    state?.let { putExtra(CalculateFragment.CALCULATE_STATE, it) }
                    routeProfile?.let { putExtra(ARGS_ROUTE_PROFILE, it) }
                },
            )
        }

        fun show(
            activity: Activity,
            destination: Destination,
            pendingType: Boolean = false
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(PENDING_TYPE, pendingType)
                },
            )
        }

        fun showAllWeb3Transaction(
            activity: Activity,
            web3FilterParams: Web3FilterParams? = null
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, WalletActivity.Destination.AllWeb3Transactions)
                    web3FilterParams?.let { putExtra(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, it) }
                },
            )
        }

        fun showWithMarket(
            activity: Activity,
            marketItem: MarketItem,
            destination: Destination,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(ARGS_MARKET, marketItem)
                },
            )
        }

        fun showWithAddress(
            activity: Activity,
            address: String,
            destination: Destination,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(ADDRESS, address)
                },
            )
        }

        fun showWithWeb3Token(
            activity: Activity,
            web3Token: Web3TokenItem,
            address: String,
            destination: Destination,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(WEB3_TOKEN, web3Token)
                    putExtra(ADDRESS, address)
                },
            )
        }
    }
}

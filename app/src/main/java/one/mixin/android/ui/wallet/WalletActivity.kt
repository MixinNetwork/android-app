package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getSerializableExtraCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.address.TransferDestinationInputFragment
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.wallet.MarketDetailsFragment.Companion.ARGS_MARKET
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_FROM_MARKET
import one.mixin.android.ui.wallet.fiatmoney.CalculateFragment
import one.mixin.android.ui.wallet.fiatmoney.FiatMoneyViewModel
import one.mixin.android.ui.wallet.fiatmoney.RouteProfile
import one.mixin.android.vo.User
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
            is Destination.Transactions -> {
                navGraph.setStartDestination(R.id.transactions_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                val fromMarket = intent.getBooleanExtra(ARGS_FROM_MARKET, false)
                navController.setGraph(navGraph, Bundle().apply {
                    putParcelable(ARGS_ASSET, token)
                    putBoolean(ARGS_FROM_MARKET, fromMarket)
                })
            }
            is Destination.Search -> {
                navGraph.setStartDestination(R.id.wallet_search_fragment)
                navController.setGraph(navGraph, null)
            }
            is Destination.SearchWeb3 -> {
                navGraph.setStartDestination(R.id.wallet_search_web3_fragment)
                val walletId = intent.getStringExtra(ARGS_WALLET_ID)
                navController.setGraph(navGraph, Bundle().apply {
                    putString(WalletSearchWeb3Fragment.ARGS_WALLET_ID, walletId)
                })
            }
            is Destination.AllTransactions -> {
                navGraph.setStartDestination(R.id.all_transactions_fragment)
                val pendingType = intent.getBooleanExtra(PENDING_TYPE, false)
                navController.setGraph(navGraph, Bundle().apply {
                    putBoolean(PENDING_TYPE, pendingType)
                })
            }
            is Destination.AllWeb3Transactions -> {
                navGraph.setStartDestination(R.id.all_web3_transactions_fragment)
                val pendingType = intent.getBooleanExtra(PENDING_TYPE, false)
                val walletId = requireNotNull(intent.getStringExtra(ARGS_WALLET_ID))
                navController.setGraph(navGraph, Bundle().apply {
                    if (pendingType) putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams(tokenFilterType = Web3TokenFilterType.PENDING, walletId = walletId))
                    else putParcelable(AllWeb3TransactionsFragment.ARGS_FILTER_PARAMS, Web3FilterParams(walletId = walletId))
                })
            }
            is Destination.Hidden -> {
                navGraph.setStartDestination(R.id.hidden_assets_fragment)
                navController.setGraph(navGraph, null)
            }
            is Destination.Web3Hidden -> {
                navGraph.setStartDestination(R.id.web3_hidden_assets_fragment)
                val walletId = intent.getStringExtra(ARGS_WALLET_ID)
                navController.setGraph(navGraph, Bundle().apply {
                    putString(Web3HiddenAssetsFragment.ARGS_WALLET_ID, walletId)
                })
            }
            is Destination.Deposit -> {
                navGraph.setStartDestination(R.id.deposit_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            is Destination.Buy -> {
                navGraph.setStartDestination(R.id.wallet_calculate)
                val state = intent.getParcelableExtraCompat(CalculateFragment.CALCULATE_STATE, FiatMoneyViewModel.CalculateState::class.java)
                routeProfile = intent.getParcelableExtraCompat(ARGS_ROUTE_PROFILE, RouteProfile::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    state?.let { s -> putParcelable(CalculateFragment.CALCULATE_STATE, s) }
                    putBoolean(
                        CalculateFragment.ARGS_IS_WEB3,
                        intent.getBooleanExtra(CalculateFragment.ARGS_IS_WEB3, false)
                    )
                    putString(
                        CalculateFragment.ARGS_WALLET_ID_FOR_CALCULATE,
                        intent.getStringExtra(CalculateFragment.ARGS_WALLET_ID_FOR_CALCULATE)
                    )
                }
                )
            }
            is Destination.Market -> {
                navGraph.setStartDestination(R.id.market_fragment_details)
                val marketItem = intent.getParcelableExtraCompat(ARGS_MARKET, MarketItem::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    marketItem?.let {
                        putParcelable(ARGS_MARKET, it)
                    }
                })
            }
            is Destination.Address -> {
                navGraph.setStartDestination(R.id.web3_address_fragment)
                val address = intent.getStringExtra(ADDRESS)
                val token = requireNotNull(intent.getParcelableExtraCompat(WEB3_TOKEN, Web3TokenItem::class.java)) { "required web3 token can not be null" }
                navController.setGraph(navGraph, Bundle().apply {
                    address?.let {
                        putString(ADDRESS, it)
                    }
                    putParcelable(WEB3_TOKEN, token)
                })
            }
            is Destination.Web3Transactions -> {
                navGraph.setStartDestination(R.id.web3_transactions_fragment)
                val web3Token = requireNotNull(intent.getParcelableExtraCompat(WEB3_TOKEN, Web3TokenItem::class.java)) { "required web3 token can not be null" }
                val address = intent.getStringExtra(ADDRESS)
                navController.setGraph(navGraph, Bundle().apply {
                    putParcelable("args_token", web3Token)
                    address?.let {
                        putString("args_address", it)
                    }
                })
            }
            is Destination.Web3TransferDestinationInput -> {
                navGraph.setStartDestination(R.id.transfer_destination_input_fragment)
                val address = intent.getStringExtra(TransferDestinationInputFragment.ARGS_ADDRESS)
                val token = intent.getParcelableExtraCompat(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
                val chain = intent.getParcelableExtraCompat(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, Web3TokenItem::class.java)
                val wallet = intent.getParcelableExtraCompat(TransferDestinationInputFragment.ARGS_WALLET, Web3Wallet::class.java)
                val asset = intent.getParcelableExtraCompat(ARGS_ASSET, TokenItem::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    asset?.let { asset-> putParcelable(ARGS_ASSET, asset) }
                    address?.let { address -> putString(TransferDestinationInputFragment.ARGS_ADDRESS, address) }
                    token?.let { token -> putParcelable(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token) }
                    chain?.let { chain -> putParcelable(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chain) }
                    wallet?.let { wallet -> putParcelable(TransferDestinationInputFragment.ARGS_WALLET, wallet) }
                })
            }
            is Destination.Input -> {
                navGraph.setStartDestination(R.id.input_fragment)
                navController.setGraph(navGraph, intent.extras)
            }
            is Destination.InputWithBiometricItem -> {
                navGraph.setStartDestination(R.id.input_fragment)
                val biometricItem = intent.getParcelableExtraCompat(InputFragment.ARGS_BIOMETRIC_ITEM, BiometricItem::class.java)
                navController.setGraph(navGraph, Bundle().apply {
                    putParcelable(InputFragment.ARGS_BIOMETRIC_ITEM, biometricItem)
                })
            }
            else -> {
                // Handle any other unexpected destination types
                throw IllegalArgumentException("Unknown destination type: $destination")
            }
        }
    }

    var routeProfile: RouteProfile? = null

    sealed class Destination : java.io.Serializable {
        object Transactions : Destination()
        object Search : Destination()
        data class SearchWeb3(val walletId: String? = null) : Destination()
        object AllTransactions : Destination()
        data class AllWeb3Transactions(val walletId: String) : Destination()
        object Hidden : Destination()
        data class Web3Hidden(val walletId: String? = null) : Destination()
        object Deposit : Destination()
        object Buy : Destination()
        object Market : Destination()
        object Address : Destination()
        object Web3Transactions : Destination()
        object Web3TransferDestinationInput : Destination()
        object Input : Destination()
        object InputWithBiometricItem : Destination()
    }

    companion object {
        const val DESTINATION = "destination"
        const val ASSET = "ASSET"
        const val BUY = "buy"
        const val ARGS_ROUTE_PROFILE = "args_route_profile"
        const val ADDRESS = "address"
        const val WEB3_TOKEN = "web3_token"
        const val PENDING_TYPE = "pending_type"
        const val ARGS_WALLET_ID = "args_wallet_id"

        fun navigateToWalletActivity(activity: Activity, address: String?, token: Web3TokenItem, chain: Web3TokenItem, wallet: Web3Wallet) {
            val intent = Intent(activity, WalletActivity::class.java).apply {
                putExtra(TransferDestinationInputFragment.ARGS_ADDRESS, address)
                putExtra(TransferDestinationInputFragment.ARGS_WEB3_TOKEN, token)
                putExtra(TransferDestinationInputFragment.ARGS_CHAIN_TOKEN, chain)
                putExtra(TransferDestinationInputFragment.ARGS_WALLET, wallet)
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
            isWeb3: Boolean,
            state: FiatMoneyViewModel.CalculateState?,
            routeProfile: RouteProfile?,
            walletId: String? = null,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Buy)
                    state?.let { putExtra(CalculateFragment.CALCULATE_STATE, it) }
                    routeProfile?.let { putExtra(ARGS_ROUTE_PROFILE, it) }
                    putExtra(CalculateFragment.ARGS_IS_WEB3, isWeb3)
                    walletId?.let { putExtra(CalculateFragment.ARGS_WALLET_ID_FOR_CALCULATE, it) }
                },
            )
        }

        fun show(
            activity: Activity,
            destination: Destination,
            pendingType: Boolean = false,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(PENDING_TYPE, pendingType)
                    if (destination is Destination.AllWeb3Transactions) {
                        putExtra(ARGS_WALLET_ID, destination.walletId)
                    } else if (destination is Destination.Web3Hidden) {
                        putExtra(ARGS_WALLET_ID, destination.walletId)
                    } else if (destination is Destination.SearchWeb3) {
                        putExtra(ARGS_WALLET_ID, destination.walletId)
                    }
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
            address: String?,
            web3TokenItem: Web3TokenItem,
            destination: Destination?,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, destination)
                    putExtra(WEB3_TOKEN, web3TokenItem)
                    putExtra(ADDRESS, address)
                },
            )
        }

        fun showWithWeb3Token(
            activity: Activity,
            web3Token: Web3TokenItem,
            address: String?,
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

        fun showDeposit(
            activity: Activity,
            tokenItem: TokenItem,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Deposit)
                    putExtra(ASSET, tokenItem)
                },
            )
        }

        fun showInputForWeb3(
            activity: Activity,
            fromAddress: String,
            toAddress: String,
            web3Token: Web3TokenItem,
            chainToken: Web3TokenItem,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Input)
                    putExtra(InputFragment.ARGS_FROM_ADDRESS, fromAddress)
                    putExtra(InputFragment.ARGS_TO_ADDRESS, toAddress)
                    putExtra(InputFragment.ARGS_WEB3_TOKEN, web3Token)
                    putExtra(InputFragment.ARGS_WEB3_CHAIN_TOKEN, chainToken)
                }
            )
        }

        fun showInputForAddress(
            activity: Activity,
            tokenItem: TokenItem,
            toAddress: String,
            tag: String? = null,
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Input)
                    putExtra(InputFragment.ARGS_TOKEN, tokenItem)
                    putExtra(InputFragment.ARGS_TO_ADDRESS, toAddress)
                    putExtra(InputFragment.ARGS_TO_ADDRESS_TAG, tag)
                }
            )
        }

        fun showInputForUser(
            activity: Activity,
            tokenItem: TokenItem,
            user: User
        ) {
            activity.startActivity(
                Intent(activity, WalletActivity::class.java).apply {
                    putExtra(DESTINATION, Destination.Input)
                    putExtra(InputFragment.ARGS_TOKEN, tokenItem)
                    putExtra(InputFragment.ARGS_TO_USER, user)
                }
            )
        }

        fun navigateToWalletActivity(activity: Activity, biometricItem: BiometricItem) {
            val intent = Intent(activity, WalletActivity::class.java).apply {
                putExtra(DESTINATION, Destination.InputWithBiometricItem)
                putExtra(InputFragment.ARGS_BIOMETRIC_ITEM, biometricItem)
            }
            activity.startActivity(intent)
        }
    }
}

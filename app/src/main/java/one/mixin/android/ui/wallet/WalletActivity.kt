package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getSerializableExtraCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.fiatmoney.CalculateFragment
import one.mixin.android.ui.wallet.fiatmoney.FiatMoneyViewModel
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
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            Destination.Search -> {
                navGraph.setStartDestination(R.id.wallet_search_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.AllTransactions -> {
                navGraph.setStartDestination(R.id.all_transactions_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.Hidden -> {
                navGraph.setStartDestination(R.id.hidden_assets_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.Deposit -> {
                navGraph.setStartDestination(R.id.deposit_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            Destination.Address -> {
                navGraph.setStartDestination(R.id.address_management_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            Destination.Contact -> {
                navGraph.setStartDestination(R.id.single_friend_select_fragment)
                val token = requireNotNull(intent.getParcelableExtraCompat(ASSET, TokenItem::class.java)) { "required token can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, token) })
            }
            Destination.WalletConnect -> {
                navGraph.setStartDestination(R.id.wallet_connect_fragment)
                navController.setGraph(navGraph, null)
            }
            Destination.Buy -> {
                navGraph.setStartDestination(R.id.wallet_calculate)
                val state = requireNotNull(intent.getParcelableExtraCompat(CalculateFragment.CALCULATE_STATE, FiatMoneyViewModel.CalculateState::class.java)) { "required state can not be null" }
                navController.setGraph(navGraph, Bundle().apply { putParcelable(CalculateFragment.CALCULATE_STATE, state) })
            }
        }
    }

    private val bottomAnim: Boolean by lazy {
        intent.extras?.getBoolean(BOTTOM_ANIM) ?: true
    }

    override fun finish() {
        super.finish()
        if (bottomAnim) {
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        }
    }

    enum class Destination {
        Transactions, Search, AllTransactions, Hidden, Deposit, Address, Contact, WalletConnect, Buy
    }

    companion object {
        const val DESTINATION = "destination"
        const val ASSET = "ASSET"
        const val BOTTOM_ANIM = "bottom_anim"
        const val BUY = "buy"

        fun show(
            activity: Activity,
            tokenItem: TokenItem? = null,
            bottomAnim: Boolean = true,
            buy: Boolean = false,
        ) {
            val myIntent = Intent(activity, WalletActivity::class.java)
            val bundle = Bundle()
            tokenItem?.let {
                bundle.putParcelable(ASSET, tokenItem)
            }
            bundle.putBoolean(BOTTOM_ANIM, bottomAnim)
            bundle.putBoolean(BUY, buy)
            myIntent.putExtras(bundle)
            activity.startActivity(myIntent)
            if (bottomAnim) {
                activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
            }
        }

        fun showWithToken(activity: Activity, tokenItem: TokenItem, destination: Destination) {
            activity.startActivity(Intent(activity, WalletActivity::class.java).apply {
                putExtra(DESTINATION, destination)
                putExtra(ASSET, tokenItem)
            })
        }

        fun showBuy(activity: Activity, state: FiatMoneyViewModel.CalculateState) {
            activity.startActivity(Intent(activity, WalletActivity::class.java).apply {
                putExtra(DESTINATION, Destination.Buy)
                putExtra(CalculateFragment.CALCULATE_STATE, state)
            })
        }

        fun show(activity: Activity, destination: Destination) {
            activity.startActivity(Intent(activity, WalletActivity::class.java).apply {
                putExtra(DESTINATION, destination)
            })
        }
    }
}

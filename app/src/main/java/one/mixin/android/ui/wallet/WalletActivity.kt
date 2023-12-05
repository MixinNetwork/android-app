package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.sumsub.KycState
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
        val currentAsset = asset
        if (currentAsset != null) {
            navGraph.setStartDestination(R.id.transactions_fragment) // change start destination
            navController.setGraph(navGraph, Bundle().apply { putParcelable(ARGS_ASSET, currentAsset) })
        } else if (isBuy) {
            navGraph.setStartDestination(R.id.wallet_calculate)
            navController.setGraph(navGraph, null)
        } else {
            navController.setGraph(navGraph, null)
        }
    }

    private val asset: TokenItem? by lazy {
        intent.extras?.getParcelableCompat(ASSET, TokenItem::class.java)
    }

    private val bottomAnim: Boolean by lazy {
        intent.extras?.getBoolean(BOTTOM_ANIM) ?: true
    }

    private val isBuy: Boolean by lazy {
        intent.extras?.getBoolean(BUY) ?: false
    }

    var kycState: String = KycState.INITIAL.value
    var hideGooglePay = false
    var supportCurrencies: List<Currency> = emptyList()
    var supportAssetIds: List<String> = emptyList()

    override fun finish() {
        super.finish()
        if (bottomAnim) {
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        }
    }

    companion object {
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
    }
}

package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

class WalletActivity : BlazeBaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        val account = Session.getAccount()
        if (account == null) {
            finish()
            return
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.container) as NavHostFragment?
        navController = navHostFragment!!.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_wallet)
        asset.notNullWithElse(
            {
                navGraph.startDestination = R.id.transactions_fragment
                navGraph.addArgument(ARGS_ASSET, NavArgument.Builder().setDefaultValue(it).build())
            },
            {
                navGraph.startDestination = R.id.wallet_fragment
            }
        )
        navController.graph = navGraph
        jobManager.addJobInBackground(RefreshAssetsJob())
    }

    private val asset: AssetItem? by lazy {
        intent.extras?.getParcelable<AssetItem>(ASSET)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
    }

    companion object {
        const val ASSET = "ASSET"

        fun show(
            activity: Activity,
            assetItem: AssetItem? = null
        ) {
            val myIntent = Intent(activity, WalletActivity::class.java)
            val bundle = Bundle()
            assetItem?.let {
                bundle.putParcelable(ASSET, assetItem)
            }
            myIntent.putExtras(bundle)
            activity.startActivity(myIntent)
            activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
        }
    }
}

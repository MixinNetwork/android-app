package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import javax.inject.Inject
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.RefreshSnapshotsJob
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem

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
            .findFragmentById(R.id.wallet_nav_fragment) as NavHostFragment?
        navController = navHostFragment!!.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_wallet)
        asset.notNullWithElse({
            navGraph.startDestination = R.id.transactions_fragment
            navGraph.addArgument(ARGS_ASSET, NavArgument.Builder().setDefaultValue(it).build())
            jobManager.addJobInBackground(RefreshSnapshotsJob(it.assetId))
        }, {
            navGraph.startDestination = R.id.wallet_fragment
        })
        navController.graph = navGraph
        jobManager.addJobInBackground(RefreshAssetsJob())
    }

    private val asset: AssetItem? by lazy {
        intent.extras?.getParcelable<AssetItem>(ASSET)
    }

    companion object {
        const val ASSET = "ASSET"
        const val LEFT_IN_ANIM = "left_in_anim"

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
        }
    }
}

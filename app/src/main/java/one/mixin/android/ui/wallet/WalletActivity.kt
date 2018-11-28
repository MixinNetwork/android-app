package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.work.WorkManager
import one.mixin.android.R
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.notNullElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.work.RefreshAssetsWorker
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
        navController = findNavController(R.id.wallet_nav_fragment)
        val navGraph = navController.navInflater.inflate(R.navigation.nav_wallet)
        notNullElse(asset, {
            navGraph.startDestination = R.id.transactions_fragment
            navGraph.setDefaultArguments(Bundle().apply { putParcelable(ARGS_ASSET, it) })
        }, {
            if (account.hasPin) {
                navGraph.startDestination = R.id.wallet_fragment
            } else {
                navGraph.startDestination = R.id.wallet_password_fragment
            }
        })
        navController.graph = navGraph
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>()
    }

    override fun finish() {
        super.finish()
        notNullElse(asset, {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }, {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        })
    }

    private val asset: AssetItem? by lazy {
        intent.extras?.getParcelable<AssetItem>(ASSET)
    }

    companion object {
        const val ASSET = "ASSET"
        fun show(activity: Activity, assetItem: AssetItem? = null) {
            val myIntent = Intent(activity, WalletActivity::class.java)
            assetItem?.let {
                val bundle = Bundle()
                bundle.putParcelable(ASSET, assetItem)
                myIntent.putExtras(bundle)
            }
            activity.startActivity(myIntent)
            notNullElse(assetItem, {
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, {
                activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            })
        }
    }
}

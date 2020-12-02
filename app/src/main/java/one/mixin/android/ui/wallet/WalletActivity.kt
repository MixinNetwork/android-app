package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

@AndroidEntryPoint
class WalletActivity : BlazeBaseActivity() {

    lateinit var navController: NavController

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
    }

    private val asset: AssetItem? by lazy {
        intent.extras?.getParcelable(ASSET)
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

    companion object {
        const val ASSET = "ASSET"
        const val BOTTOM_ANIM = "bottom_anim"

        fun show(
            activity: Activity,
            assetItem: AssetItem? = null,
            bottomAnim: Boolean = true,
        ) {
            val myIntent = Intent(activity, WalletActivity::class.java)
            val bundle = Bundle()
            assetItem?.let {
                bundle.putParcelable(ASSET, assetItem)
                bundle.putBoolean(BOTTOM_ANIM, bottomAnim)
            }
            myIntent.putExtras(bundle)
            activity.startActivity(myIntent)
            if (bottomAnim) {
                activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
            }
        }
    }
}

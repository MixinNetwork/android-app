package one.mixin.android.ui.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.replaceFragment
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import javax.inject.Inject

class WalletActivity : BlazeBaseActivity() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val account = Session.getAccount()
        if (account == null) {
            finish()
            return
        }
        notNullElse(asset, {
            val fragment = TransactionsFragment.newInstance(it)
            replaceFragment(fragment, R.id.container, TransactionsFragment.TAG)
        }, {
            if (account.hasPin) {
                val fragment = WalletFragment.newInstance()
                replaceFragment(fragment, R.id.container, WalletFragment.TAG)
            } else {
                val fragment = WalletPasswordFragment.newInstance()
                replaceFragment(fragment, R.id.container, WalletPasswordFragment.TAG)
            }
        })
        jobManager.addJobInBackground(RefreshAssetsJob())
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

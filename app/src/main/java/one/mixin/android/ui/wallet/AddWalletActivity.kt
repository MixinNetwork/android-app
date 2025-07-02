package one.mixin.android.ui.wallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class AddWalletActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_wallet)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, AddWalletFragment.newInstance())
                .commitNow()
        }
    }

    companion object {
        fun show(activity: AppCompatActivity) {
            activity.startActivity(android.content.Intent(activity, AddWalletActivity::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}


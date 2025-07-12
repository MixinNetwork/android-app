package one.mixin.android.ui.wallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.AddWalletSuccessEvent
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
        RxBus.listen(AddWalletSuccessEvent::class.java).observeOn(AndroidSchedulers.mainThread()).autoDispose(destroyScope).subscribe {
            finish()
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


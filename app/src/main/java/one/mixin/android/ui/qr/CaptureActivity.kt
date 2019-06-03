package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_contact.*
import one.mixin.android.R
import one.mixin.android.extension.FLAGS_FULLSCREEN
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.qr.CaptureFragment.Companion.ARGS_FOR_ACCOUNT_NAME
import one.mixin.android.ui.qr.CaptureFragment.Companion.ARGS_FOR_ADDRESS
import one.mixin.android.ui.qr.CaptureFragment.Companion.newInstance

class CaptureActivity : BlazeBaseActivity() {

    private lateinit var captureFragment: CaptureFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        captureFragment = when {
            intent.hasExtra(ARGS_FOR_ADDRESS) -> newInstance(true)
            intent.hasExtra(ARGS_FOR_ACCOUNT_NAME) -> newInstance(forAccountName = true)
            else -> newInstance()
        }
        replaceFragment(captureFragment, R.id.container, CaptureFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        container.postDelayed({
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }

    companion object {
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L

        fun show(
            activity: Activity,
            actionWithIntent: ((intent: Intent) -> Unit)? = null
        ) {
            Intent(activity, CaptureActivity::class.java).apply {
                activity.overridePendingTransition(R.anim.slide_in_bottom, 0)
                if (actionWithIntent == null) {
                    activity.startActivity(this)
                } else {
                    actionWithIntent.invoke(this)
                }
            }
        }
    }
}

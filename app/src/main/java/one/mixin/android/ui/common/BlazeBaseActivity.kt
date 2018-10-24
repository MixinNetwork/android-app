package one.mixin.android.ui.common

import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_RESUME
import one.mixin.android.job.BlazeMessageService.Companion.startService
import one.mixin.android.util.Session

abstract class BlazeBaseActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        if (Session.checkToken()) {
            startService(this, ACTION_ACTIVITY_RESUME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Session.checkToken()) {
            startService(this, ACTION_ACTIVITY_PAUSE)
        }
    }
}
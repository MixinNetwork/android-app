package one.mixin.android.ui.common

import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_RESUME
import one.mixin.android.job.BlazeMessageService.Companion.startService

abstract class BlazeBaseActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        startService(this, ACTION_ACTIVITY_RESUME)
    }

    override fun onPause() {
        super.onPause()
        startService(this, ACTION_ACTIVITY_PAUSE)
    }
}
package one.mixin.android.ui.common

import one.mixin.android.MixinApplication
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_RESUME
import one.mixin.android.job.BlazeMessageService.Companion.startService
import one.mixin.android.session.Session

abstract class BlazeBaseActivity : BaseActivity() {
    companion object {
        private val resumedActivities = mutableSetOf<BlazeBaseActivity>()
    }

    override fun onResume() {
        super.onResume()
        resumedActivities.add(this)
        if (Session.checkToken() && MixinApplication.get().isOnline.get()) {
            startService(this, ACTION_ACTIVITY_RESUME)
        }
    }

    override fun onPause() {
        super.onPause()
        resumedActivities.remove(this)
        if (resumedActivities.isEmpty() && Session.checkToken() && MixinApplication.get().isOnline.get()) {
            startService(this, ACTION_ACTIVITY_PAUSE)
        }
    }
}

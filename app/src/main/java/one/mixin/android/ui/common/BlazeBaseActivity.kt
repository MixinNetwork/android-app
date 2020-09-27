package one.mixin.android.ui.common

import one.mixin.android.MixinApplication
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_RESUME
import one.mixin.android.job.BlazeMessageService.Companion.startService
import one.mixin.android.session.Session

abstract class BlazeBaseActivity : BaseActivity() {

    override fun onResume() {
        super.onResume()
        if (Session.checkToken() && MixinApplication.get().onlining.get()) {
            startService(this, ACTION_ACTIVITY_RESUME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Session.checkToken() && MixinApplication.get().onlining.get()) {
            startService(this, ACTION_ACTIVITY_PAUSE)
        }
    }
}

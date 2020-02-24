package one.mixin.android.util.language

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import one.mixin.android.ui.common.BaseActivity

internal class LingverActivityLifecycleCallbacks(private val lingver: Lingver) : ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        lingver.setLocaleInternal(activity)
        lingver.resetActivityTitle(activity)
        if (activity is BaseActivity) {
            activity.lastLang = lingver.getLanguage()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is BaseActivity) {
            val curlang = Lingver.getInstance().getLanguage()
            if (activity.lastLang != curlang) {
                activity.recreate()
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        if (activity is BaseActivity) {
            activity.lastLang = Lingver.getInstance().getLanguage()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

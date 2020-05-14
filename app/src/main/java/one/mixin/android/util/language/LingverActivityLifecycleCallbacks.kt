package one.mixin.android.util.language

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import one.mixin.android.extension.getCurrentThemeId
import one.mixin.android.ui.common.BaseActivity

internal class LingverActivityLifecycleCallbacks(
    private val lingver: Lingver,
    private val callback: (Activity) -> Unit
) : ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        callback.invoke(activity)
        if (activity is BaseActivity) {
            activity.lastLang = lingver.getLanguage()
            activity.lastThemeId = activity.getCurrentThemeId()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity is BaseActivity) {
            val curlang = Lingver.getInstance().getLanguage()
            val themeId = activity.getCurrentThemeId()
            if (activity.lastLang != curlang || activity.lastThemeId != themeId) {
                activity.recreate()
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        if (activity is BaseActivity) {
            activity.lastLang = Lingver.getInstance().getLanguage()
            activity.lastThemeId = activity.getCurrentThemeId()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}

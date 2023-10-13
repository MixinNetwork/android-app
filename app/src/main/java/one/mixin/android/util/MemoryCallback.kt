package one.mixin.android.util

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import timber.log.Timber

class MemoryCallback : ComponentCallbacks2 {
    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.w(newConfig.toString())
    }

    override fun onLowMemory() {
        Timber.w("onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        Timber.w("onTrimMemory level: $level")
    }
}

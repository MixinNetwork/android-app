package one.mixin.android.util.language

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration

internal class LingverApplicationCallbacks(
    private val context: Context,
    private val lingver: Lingver
) : ComponentCallbacks {

    override fun onConfigurationChanged(newConfig: Configuration) {
        lingver.setLocaleInternal(context)
    }

    override fun onLowMemory() {}
}

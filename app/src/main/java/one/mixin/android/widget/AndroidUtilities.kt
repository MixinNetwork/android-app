package one.mixin.android.widget

import android.util.DisplayMetrics
import one.mixin.android.MixinApplication
import org.jetbrains.anko.dip

object AndroidUtilities {
    var displayMetrics = DisplayMetrics()
    fun getPixelsInCM(cm: Float, isX: Boolean): Float {
        return cm / 2.54f * if (isX) displayMetrics.xdpi else displayMetrics.ydpi
    }

    fun dp(value: Float): Int {
        return MixinApplication.appContext.dip(value)
    }
}

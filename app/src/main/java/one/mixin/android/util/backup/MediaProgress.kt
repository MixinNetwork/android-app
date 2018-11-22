package one.mixin.android.util.backup

import androidx.lifecycle.LiveData
import one.mixin.android.MixinApplication
import org.jetbrains.anko.runOnUiThread

class MediaProgress : LiveData<Int?>() {
    var progress: Int? = null
        set(value) {
            MixinApplication.appContext.runOnUiThread {
                if (field != value) {
                    field = value
                    setValue(value)
                }
            }
        }
}
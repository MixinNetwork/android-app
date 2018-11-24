package one.mixin.android.util.backup

import androidx.lifecycle.LiveData
import one.mixin.android.MixinApplication
import org.jetbrains.anko.runOnUiThread

class BackupLiveData : LiveData<Boolean>() {
    var ing: Boolean = false
        private set(value) {
            MixinApplication.appContext.runOnUiThread {
                if (field != value) {
                    field = value
                    setValue(value)
                }
            }
        }
    var result: Result? = null
        private set

    fun setResult(ing: Boolean, result: Result?) {
        this.result = result
        this.ing = ing
    }

    fun start() {
        setResult(true, null)
    }
}

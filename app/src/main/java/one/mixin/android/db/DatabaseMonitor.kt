package one.mixin.android.db

import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.util.PropertyHelper
import timber.log.Timber

object DatabaseMonitor {

    private val logSet = HashMap<String, Long>()
    private var str = StringBuffer()
    private var enable = false

    fun reset() {
        MixinApplication.get().applicationScope.launch {
            enable = PropertyHelper.findValueByKey(Constants.Debug.DB_DEBUG_LOGS)
                ?.toBoolean() ?: false
        }
    }

    init {
        reset()
    }

    fun monitor(sqlQuery: String) {
        if (!BuildConfig.DEBUG || !enable) return
        val sql = sqlQuery.trim()
        val currentThreadName = Thread.currentThread().name
        if (sql.startsWith("BEGIN")) {
            str.append("-------<BEGIN>-------($currentThreadName)\n")
            if (logSet[currentThreadName] == null) {
                logSet[currentThreadName] = System.currentTimeMillis()
            }
            // Timber.e("$currentThreadName $sql")
        } else if (sql.startsWith("END TRANSACTION") || sql.startsWith("TRANSACTION SUCCESSFUL")) {
            logSet[currentThreadName]?.let {
                val time = System.currentTimeMillis() - it
                if (time > 50) {
                    Timber.e("$currentThreadName It takes $time milliseconds")
                    str.append("$sql\n")
                    str.append("It takes $time milliseconds\n")
                    str.append("--------<END>--------($currentThreadName)\n\n")
                    Timber.e(str.toString())
                    // reportException("It takes $time milliseconds\n $str", LogExtension())
                    str = StringBuffer()
                }
                logSet.remove(currentThreadName)
            }
        } else if (!sql.startsWith("SELECT")) {
            str.append("$currentThreadName[$sql]\n")
        } else {
            // Timber.w("$currentThreadName[$sql]")
        }
    }
}

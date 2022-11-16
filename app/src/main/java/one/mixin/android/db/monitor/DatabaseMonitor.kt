package one.mixin.android.db.monitor

import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.util.PropertyHelper
import timber.log.Timber

object DatabaseMonitor {

    private val databaseMap = HashMap<String, MonitorData>()
    var enable = true
        private set

    fun reset() {
        MixinApplication.get().applicationScope.launch {
            enable = PropertyHelper.findValueByKey(Constants.Debug.DB_DEBUG_LOGS)
                ?.toBoolean() ?: true
        }
    }

    init {
        reset()
    }


    fun monitor(sqlQuery: String, args: List<Any?>) {
        if (!BuildConfig.DEBUG) return
        val sql = sqlQuery.trim()
        val currentThreadName = Thread.currentThread().name
        var monitorData = databaseMap[currentThreadName]
        if (monitorData == null && sql.startsWith("BEGIN")) {
            monitorData = MonitorData(System.currentTimeMillis(), sql.contains("DEFERRED", true))
            databaseMap[currentThreadName] = monitorData
        }
        monitorData ?: return
        monitorData.append(sql, args)
        if (sql.startsWith("END TRANSACTION")) {
            monitorData.log(currentThreadName)
            databaseMap.remove(currentThreadName)
        }
    }

    fun log(log: String) {
        if (!BuildConfig.DEBUG || !enable) return
        Timber.wtf(log)
    }

}

class SlowSqlExtension : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

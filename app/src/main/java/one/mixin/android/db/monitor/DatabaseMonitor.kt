package one.mixin.android.db.monitor

import one.mixin.android.BuildConfig
import timber.log.Timber

object DatabaseMonitor {

    private val databaseMap = HashMap<String, MonitorData>()

    fun monitor(sqlQuery: String, args: List<Any?>) {
        if (!BuildConfig.DEBUG) return
        val sql = sqlQuery.trim()
        val currentThreadName = Thread.currentThread().name
        var monitorData = databaseMap[currentThreadName]
        if (monitorData == null) {
            monitorData = MonitorData(System.currentTimeMillis())
            databaseMap[currentThreadName] = monitorData
        }
        monitorData.append(sql, args)
        if (sql.startsWith("END TRANSACTION")) {
            monitorData.log(currentThreadName)
            databaseMap.remove(currentThreadName)
        }
    }
}

class SlowSqlExtension : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

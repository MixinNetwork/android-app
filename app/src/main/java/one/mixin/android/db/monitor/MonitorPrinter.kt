package one.mixin.android.db.monitor

import one.mixin.android.BuildConfig

object MonitorPrinter {

    private val databaseMap = HashMap<String, MonitorData>()

    fun print(sqlQuery: String, args: List<Any?>) {
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
}

package one.mixin.android.db

import one.mixin.android.BuildConfig
import one.mixin.android.util.reportException
import timber.log.Timber

object DatabaseMonitor {

    private val logSet = HashMap<String, Long>()
    private var str = StringBuffer()

    fun monitor(sqlQuery: String, args: List<Any?>) {
        if (!BuildConfig.DEBUG) return
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
                val timeDiff = System.currentTimeMillis() - it
                if (timeDiff > 500) {
                    Timber.e("$currentThreadName It takes $timeDiff milliseconds")
                    str.append("$sql $args \n")
                    str.append("It takes $timeDiff milliseconds\n")
                    str.append("--------<END>--------($currentThreadName)\n\n")
                    Timber.e(str.toString())
                    reportException("It takes $timeDiff milliseconds\n $str", SlowSqlExtension())
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

class SlowSqlExtension : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

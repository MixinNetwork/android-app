package one.mixin.android.db.monitor

import one.mixin.android.util.reportException
import timber.log.Timber

class MonitorData(private var createdTime: Long, private var isDeferred: Boolean = false) {
    private val statements = mutableListOf<String>()
    fun append(sql: String, args: List<Any?>) {
        if (isDeferred && !sql.startsWith("BEGIN") && !sql.startsWith("END")) {
            createdTime = System.currentTimeMillis()
            isDeferred = isDeferred.not()
        }
        statements.add(sql)
    }

    override fun toString(): String {
        val timeDiff = (System.currentTimeMillis() - createdTime)
        val content = statements.joinToString("\n")
        if (timeDiff > 500) {
            reportException("It takes $timeDiff milliseconds\n $content", SlowSqlExtension())
        }
        return "$content\n**** $timeDiff milliseconds****\n\n"
    }

    fun log(currentThreadName: String, diff: Long = 200) {
        val timeDiff = System.currentTimeMillis() - createdTime
        if (timeDiff >= diff) {
            Timber.e("******$currentThreadName******\n $this")
        }
    }
}

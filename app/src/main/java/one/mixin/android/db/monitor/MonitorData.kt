package one.mixin.android.db.monitor

import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.reportException
import timber.log.Timber

class MonitorData(private var createdTime: Long, private var isDeferred: Boolean = false) {
    private val statements = mutableListOf<String>()
    fun append(sql: String, args: List<Any?>) {
        if (isDeferred && !sql.startsWith("BEGIN") && !sql.startsWith("END")) {
            createdTime = System.currentTimeMillis()
            isDeferred = isDeferred.not()
            statements.add("run ${nowInUtc()}")
        } else if (!isDeferred && sql.startsWith("BEGIN")) {
            statements.add("run ${nowInUtc()}")
        }
        statements.add(sql)
        if (sql.startsWith("END")) {
            statements.add("end ${nowInUtc()}")
        }
        // if (args.isEmpty()) statements.add(sql)
        // else statements.add("$sql $args")
    }

    override fun toString(): String {
        val timeDiff = (System.currentTimeMillis() - createdTime)
        val content = statements.joinToString("\n")
        if (timeDiff > 500) {
            reportException("It takes $timeDiff milliseconds\n $content", SlowSqlExtension())
        }
        return "$content\n**** $timeDiff milliseconds****\n\n"
    }

    fun log(currentThreadName: String, diff: Long = 500) {
        val timeDiff = System.currentTimeMillis() - createdTime
        if (timeDiff >= diff) {
            Timber.e("******$currentThreadName******\n $this")
        }
    }
}

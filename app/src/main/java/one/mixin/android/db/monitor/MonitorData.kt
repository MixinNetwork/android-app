package one.mixin.android.db.monitor

import one.mixin.android.util.reportException
import timber.log.Timber

class MonitorData(private val createdTime: Long) {
    private val statements = mutableListOf<String>()
    fun append(sql: String, args: List<Any?>) {
        statements.add(sql)
        // if (args.isEmpty()) statements.add(sql)
        // else statements.add("$sql $args")
    }

    override fun toString(): String {
        val timeDiff = (System.currentTimeMillis() - createdTime)
        val content = statements.joinToString("\n")
        if (timeDiff > 500){
            reportException("It takes $timeDiff milliseconds\n $content", SlowSqlExtension())
        }
        return "$content\n**** $timeDiff milliseconds****\n\n"
    }

    fun log(currentThreadName: String, diff:Long = 200) {
        val timeDiff = System.currentTimeMillis() - createdTime
        if (timeDiff >= diff) {
            Timber.e("******$currentThreadName******\n $this")
        }
    }
}
package one.mixin.android.db

import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.db.monitor.MonitorPrinter
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.session.Session
import timber.log.Timber

object DatabaseMonitor {
    private val logSet = HashMap<String, Long>()
    private var str = StringBuffer()
    var enable = false
        private set

    fun reset() {
        MixinApplication.get().applicationScope.launch {
            if (Session.getAccount() == null) {
                enable = false
                return@launch
            }
            enable =
                runCatching {
                    PropertyHelper.findValueByKey(Constants.Debug.DB_DEBUG_LOGS, false)
                }.getOrElse {
                    Timber.w(it, "Skip DatabaseMonitor reset before account scope is ready")
                    false
                }
        }
    }

    init {
        reset()
    }

    fun monitor(
        sqlQuery: String,
        args: List<Any?>,
    ) {
        if (!enable) return
        MonitorPrinter.print(sqlQuery, args)
    }

    fun log(log: String) {
        if (!enable) return
        Timber.wtf(log)
    }
}

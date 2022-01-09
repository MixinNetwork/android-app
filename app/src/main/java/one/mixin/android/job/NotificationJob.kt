package one.mixin.android.job

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.extension.notificationManager
import one.mixin.android.vo.Message

@Deprecated(
    message = "moved to NotificationGenerator, and keep this empty class for old version deserialize",
    replaceWith = ReplaceWith("NotificationGenerator"),
    level = DeprecationLevel.ERROR
)
class NotificationJob(val message: Message, private val userMap: Map<String, String>? = null, private val force: Boolean = false) : BaseJob(
    Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("notification_group")
) {
    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.notificationManager
    }
}

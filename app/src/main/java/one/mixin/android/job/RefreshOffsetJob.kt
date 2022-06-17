package one.mixin.android.job

import androidx.collection.ArrayMap
import com.birbit.android.jobqueue.Params
import one.mixin.android.db.makeMessageStatus
import one.mixin.android.extension.getEpochNano
import one.mixin.android.vo.Offset
import one.mixin.android.vo.STATUS_OFFSET
import java.util.UUID

var pendingMessageStatusMap = ArrayMap<String, String>()

class RefreshOffsetJob : MixinJob(
    Params(PRIORITY_UI_HIGH)
        .setSingleId(GROUP).requireNetwork(),
    UUID.randomUUID().toString()
) {
    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshOffsetJob"
    }

    private val firstInstallTime by lazy {
        applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).firstInstallTime * 1000000
    }

    override fun onRun() {

        val statusOffset = offsetDao.getStatusOffset()
        var status = statusOffset?.getEpochNano() ?: firstInstallTime
        while (true) {
            val response = messageService.messageStatusOffset(status).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val blazeMessages = response.data!!
                if (blazeMessages.isEmpty()) {
                    break
                }
                for (m in blazeMessages) {
                    pendingMessageStatusMap[m.messageId] = m.status
                    messageDao.makeMessageStatus(m.status, m.messageId)
                    offsetDao.insert(Offset(STATUS_OFFSET, m.updatedAt))
                }
                if (blazeMessages.isNotEmpty() && blazeMessages.last().updatedAt.getEpochNano() == status) {
                    break
                }
                status = blazeMessages.last().updatedAt.getEpochNano()
            } else {
                break
            }
        }
    }
}

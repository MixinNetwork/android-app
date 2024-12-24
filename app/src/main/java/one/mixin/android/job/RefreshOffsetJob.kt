package one.mixin.android.job

import android.util.LruCache
import com.birbit.android.jobqueue.Params
import one.mixin.android.db.makeMessageStatus
import one.mixin.android.extension.getEpochNano
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Offset
import one.mixin.android.vo.STATUS_OFFSET
import java.util.UUID

var pendingMessageStatusLruCache = LruCache<String, String>(800)

class RefreshOffsetJob : MixinJob(
    Params(PRIORITY_UI_HIGH)
        .setSingleId(GROUP).requireNetwork(),
    UUID.randomUUID().toString(),
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
        val statusOffset = offsetDao().getStatusOffset()
        var status = statusOffset?.getEpochNano() ?: firstInstallTime

        while (true) {
            val response = messageService.messageStatusOffset(status).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val blazeMessages = response.data!!
                if (blazeMessages.isEmpty()) {
                    break
                }
                for (m in blazeMessages) {
                    val callback = block@{
                        val mh = messageHistoryDao().findMessageHistoryById(m.messageId)
                        if (mh != null) {
                            return@block
                        }
                        val pendingMessageStatus = pendingMessageStatusLruCache[m.messageId]
                        if (pendingMessageStatus != null) {
                            val currentStatus = MessageStatus.entries.firstOrNull { it.name == m.status }?.ordinal ?: return@block
                            val localStatus = MessageStatus.entries.firstOrNull { it.name == pendingMessageStatus }?.ordinal ?: return@block
                            if (currentStatus > localStatus) {
                                pendingMessageStatusLruCache.put(m.messageId, m.status)
                            }
                        } else {
                            pendingMessageStatusLruCache.put(m.messageId, m.status)
                        }
                    }
                    pendingDatabase().makeMessageStatus(m.status, m.messageId, callback)
                    database().makeMessageStatus(m.status, m.messageId, callback)
                    offsetDao().insert(Offset(STATUS_OFFSET, m.updatedAt))
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

package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.extension.getEpochNano
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.Offset
import one.mixin.android.vo.STATUS_OFFSET
import java.util.UUID

class RefreshOffsetJob : MixinJob(Params(PRIORITY_UI_HIGH)
    .setSingleId(GROUP).requireNetwork(), UUID.randomUUID().toString()) {
    override fun cancel() {
    }

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshOffsetJob"
    }

    override fun onRun() {
        val statusOffset = offsetDao.getStatusOffset()
        var status = statusOffset?.getEpochNano() ?: nowInUtc().getEpochNano()
        while (true) {
            val response = messageService.messageStatusOffset(status).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val blazeMessages = response.data!!
                if (blazeMessages.count() == 0) {
                    break
                }
                for (m in blazeMessages) {
                    makeMessageStatus(m.status, m.messageId)
                    offsetDao.insert(Offset(STATUS_OFFSET, m.updatedAt))
                }
                if (blazeMessages.count() > 0 && blazeMessages.last().updatedAt.getEpochNano() == status) {
                    break
                }
                status = blazeMessages.last().updatedAt.getEpochNano()
            } else {
                break
            }
        }
    }
}
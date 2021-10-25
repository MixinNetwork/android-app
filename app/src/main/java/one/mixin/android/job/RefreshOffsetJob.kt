package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants.STATUS_OFFSET
import one.mixin.android.extension.getEpochNano
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.Property
import java.util.UUID

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
        val statusOffset = propertyDao.findValueByKeySync(STATUS_OFFSET)
        var status = statusOffset?.getEpochNano() ?: firstInstallTime
        while (true) {
            val response = messageService.messageStatusOffset(status).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val blazeMessages = response.data!!
                if (blazeMessages.count() == 0) {
                    break
                }
                for (m in blazeMessages) {
                    makeMessageStatus(m.status, m.messageId)
                    propertyDao.insert(Property(STATUS_OFFSET, m.updatedAt, nowInUtc()))
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

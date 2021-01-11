package one.mixin.android.job

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.ui.common.message.SendMessageHelper
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMessage
import timber.log.Timber
import java.util.UUID

class JobDeleteJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "job_delete"
        const val JOBS_DELETE = "job_delete"
        const val JOBS_DELETE_LAST = "job_delete_last"
    }

    override fun onRun() {
        deleteJobs()
    }

    private fun deleteJobs() = runBlocking {
        val preferences = MixinApplication.appContext.defaultSharedPreferences
        val count = jobDao.getJobsCount()
        val lastCount = preferences.getLong(JOBS_DELETE_LAST, count.toLong())
        if (lastCount - count > 1000000) {
            val message = createMessage(
                UUID.randomUUID().toString(),
                "c1183ad8-e47a-34d4-a7bd-6313dd936bce",
                "639ec50a-d4f1-4135-8624-3c71189dcdcc",
                MessageCategory.PLAIN_TEXT.name,
                "surplus $count",
                nowInUtc(),
                MessageStatus.SENDING.name
            )
            jobManager.addJob(SendMessageJob(message))
        }
        if (count > 0) {
            repeat(10) {
                jobDao.clearAckJobs()
            }
            Timber.e("clear job 10000")
            jobManager.addJob(JobDeleteJob())
        } else {
            val message = createMessage(
                UUID.randomUUID().toString(),
                "c1183ad8-e47a-34d4-a7bd-6313dd936bce",
                "639ec50a-d4f1-4135-8624-3c71189dcdcc",
                MessageCategory.PLAIN_TEXT.name,
                "Done!!!",
                nowInUtc(),
                MessageStatus.SENDING.name
            )
            jobManager.addJob(SendMessageJob(message))
            preferences.putBoolean(JOBS_DELETE, true)
        }
    }
}

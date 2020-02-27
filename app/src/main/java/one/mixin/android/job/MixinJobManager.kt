package one.mixin.android.job

import com.birbit.android.jobqueue.CancelResult
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.TagConstraint
import com.birbit.android.jobqueue.config.Configuration
import java.util.concurrent.ConcurrentHashMap

class MixinJobManager(configuration: Configuration) : JobManager(configuration) {

    private val map: ConcurrentHashMap<String, MixinJob> = ConcurrentHashMap()

    fun saveJob(job: Job) {
        if (job is MixinJob) {
            map[job.mixinJobId] = job
        }
    }

    fun removeJobByMixinJobId(mixinJobId: String) {
        map.remove(mixinJobId)
    }

    fun cancelJobByMixinJobId(mixinJobId: String) {
        val mixinJob = findJobByMixinJobId(mixinJobId)
        if (mixinJob == null) {
            cancelJobsInBackground(CancelResult.AsyncCancelCallback { result ->
                val job = result.cancelledJobs.find { it.tags?.contains(mixinJobId) == true }
                (job as? MixinJob)?.cancel()
            }, TagConstraint.ANY, mixinJobId)
        } else {
            mixinJob.cancel()
            cancelJobsInBackground(null, TagConstraint.ANY, mixinJobId)
        }
    }

    fun findJobByMixinJobId(mixinJobId: String): MixinJob? = map[mixinJobId]

    fun cancelAllJob() {
        for (job in map.values) {
            job.cancel()
        }
        map.clear()
    }
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import java.util.concurrent.ConcurrentHashMap

class MixinJobManager(configuration: Configuration) : JobManager(configuration) {

    private val map: ConcurrentHashMap<String, MixinJob> by lazy {
        ConcurrentHashMap<String, MixinJob>()
    }

    fun saveJob(job: Job) {
        if (job is MixinJob) {
            map[job.jobId] = job
        }
    }

    fun removeJob(id: String) {
        map.remove(id)
    }

    fun cancelJobById(id: String) {
        findJobById(id)?.cancel()
    }

    fun findJobById(id: String): MixinJob? = map[id]

    fun cancelAllJob() {
        for (job in map.values) {
            job.cancel()
        }
        map.clear()
    }
}

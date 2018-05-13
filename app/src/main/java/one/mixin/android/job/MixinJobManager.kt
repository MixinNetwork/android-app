package one.mixin.android.job

import android.util.ArrayMap
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration

class MixinJobManager(configuration: Configuration) : JobManager(configuration) {

    private val map: ArrayMap<String, MixinJob> by lazy {
        ArrayMap<String, MixinJob>()
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

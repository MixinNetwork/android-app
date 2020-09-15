package one.mixin.android.job

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyJobService : FrameworkJobSchedulerService() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun getJobManager(): JobManager {
        return jobManager
    }
}

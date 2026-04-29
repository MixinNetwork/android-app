package one.mixin.android.job;

import android.content.Context;

import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.Scheduler;

public final class JobQueueSchedulerFactory {
    private JobQueueSchedulerFactory() {
    }

    public static Scheduler create(
        Context appContext,
        Class<? extends FrameworkJobSchedulerService> serviceClass
    ) {
        return FrameworkJobSchedulerService.createSchedulerFor(appContext, serviceClass);
    }
}

package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.RefreshUserWorker

@Subcomponent
interface RefreshUserWorkerSubcomponent : AndroidInjector<RefreshUserWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshUserWorker>()
}
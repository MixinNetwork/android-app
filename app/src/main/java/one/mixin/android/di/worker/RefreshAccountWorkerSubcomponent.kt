package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.RefreshAccountWorker

@Subcomponent
interface RefreshAccountWorkerSubcomponent : AndroidInjector<RefreshAccountWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshAccountWorker>()
}
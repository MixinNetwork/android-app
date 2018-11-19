package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.work.RefreshContactWorker

@Subcomponent
interface RefreshContactWorkerSubcomponent : AndroidInjector<RefreshContactWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshContactWorker>()
}
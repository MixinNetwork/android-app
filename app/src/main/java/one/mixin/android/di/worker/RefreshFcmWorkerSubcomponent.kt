package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.RefreshFcmWorker

@Subcomponent
interface RefreshFcmWorkerSubcomponent : AndroidInjector<RefreshFcmWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshFcmWorker>()
}
package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.RefreshAssetsWorker

@Subcomponent
interface RefreshAssetsWorkerSubcomponent : AndroidInjector<RefreshAssetsWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshAssetsWorker>()
}
package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.work.RefreshTopAssetsWorker

@Subcomponent
interface RefreshTopAssetsWorkerSubcomponent : AndroidInjector<RefreshTopAssetsWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshTopAssetsWorker>()
}
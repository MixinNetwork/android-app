package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.RemoveStickersWorker

@Subcomponent
interface RemoveStickersWorkerSubcomponent : AndroidInjector<RemoveStickersWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RemoveStickersWorker>()
}
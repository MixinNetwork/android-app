package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.work.RefreshUserSnapshotsWorker

@Subcomponent
interface RefreshUserSnapshotsWorkerSubcomponent : AndroidInjector<RefreshUserSnapshotsWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RefreshUserSnapshotsWorker>()
}
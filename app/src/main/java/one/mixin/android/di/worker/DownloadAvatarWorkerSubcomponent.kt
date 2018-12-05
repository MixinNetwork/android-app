package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.worker.DownloadAvatarWorker
import one.mixin.android.worker.GenerateAvatarWorker

@Subcomponent
interface DownloadAvatarWorkerSubcomponent : AndroidInjector<DownloadAvatarWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<DownloadAvatarWorker>()
}
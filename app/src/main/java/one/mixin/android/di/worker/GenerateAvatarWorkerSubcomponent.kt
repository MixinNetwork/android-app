package one.mixin.android.di.worker

import dagger.Subcomponent
import dagger.android.AndroidInjector
import one.mixin.android.work.GenerateAvatarWorker

@Subcomponent
interface GenerateAvatarWorkerSubcomponent : AndroidInjector<GenerateAvatarWorker> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<GenerateAvatarWorker>()
}
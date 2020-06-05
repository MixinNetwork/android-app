package one.mixin.android.di.worker

import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import one.mixin.android.MixinApplication
import javax.inject.Singleton

@Module(includes = [WorkerAssistedModule::class, WorkersModuleBinds::class])
class WorkerModule {
    @Provides
    @Singleton
    fun provideWorkerManager(): WorkManager = WorkManager.getInstance(MixinApplication.appContext)
}

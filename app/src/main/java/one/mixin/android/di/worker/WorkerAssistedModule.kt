package one.mixin.android.di.worker

import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Module

@AssistedModule
@Module(includes = [AssistedInject_WorkerAssistedModule::class])
interface WorkerAssistedModule
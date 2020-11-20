package one.mixin.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent

@Module(includes = [BaseDbModule::class])
@InstallIn(ApplicationComponent::class)
object TestAppModule

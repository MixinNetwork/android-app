package one.mixin.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(includes = [BaseDbModule::class])
@InstallIn(SingletonComponent::class)
object TestAppModule

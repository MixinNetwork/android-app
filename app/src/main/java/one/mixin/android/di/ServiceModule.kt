package one.mixin.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.fcm.FcmService
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MyJobService
import one.mixin.android.job.SendService

@Module abstract class ServiceModule {
    @ContributesAndroidInjector internal abstract fun contributeFcmService(): FcmService
    @ContributesAndroidInjector internal abstract fun contributeMyJobService(): MyJobService
    @ContributesAndroidInjector internal abstract fun contributeMessageService(): BlazeMessageService
    @ContributesAndroidInjector internal abstract fun contributeSendService(): SendService
}
package one.mixin.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.job.SendService
import one.mixin.android.fcm.FcmInstanceIDService
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MyJobService
import one.mixin.android.job.UploadContactsService

@Module abstract class ServiceModule {
    @ContributesAndroidInjector internal abstract fun contributeUploadContactsService(): UploadContactsService
    @ContributesAndroidInjector internal abstract fun contributeFcmService(): FcmInstanceIDService
    @ContributesAndroidInjector internal abstract fun contributeMyJobService(): MyJobService
    @ContributesAndroidInjector internal abstract fun contributeMessageService(): BlazeMessageService
    @ContributesAndroidInjector internal abstract fun contributeSendService(): SendService
}
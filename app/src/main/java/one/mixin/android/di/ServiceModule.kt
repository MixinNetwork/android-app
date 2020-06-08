package one.mixin.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.fcm.FcmService
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MyJobService
import one.mixin.android.job.SendService
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService

@Module abstract class ServiceModule {
    @ContributesAndroidInjector internal abstract fun contributeFcmService(): FcmService
    @ContributesAndroidInjector internal abstract fun contributeMyJobService(): MyJobService
    @ContributesAndroidInjector internal abstract fun contributeMessageService(): BlazeMessageService
    @ContributesAndroidInjector internal abstract fun contributeSendService(): SendService
    @ContributesAndroidInjector internal abstract fun contributeVoiceCallService(): VoiceCallService
    @ContributesAndroidInjector internal abstract fun contributeGroupCallService(): GroupCallService
}

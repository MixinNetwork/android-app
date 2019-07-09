package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.landing.MobileFragment
import one.mixin.android.ui.landing.VerificationFragment
import one.mixin.android.ui.setting.VerificationEmergencyFragment
import one.mixin.android.ui.setting.VerificationEmergencyIdFragment

@Module
abstract class LandingActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeMobileFragment(): MobileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationFragment(): VerificationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationEmergencyFragment(): VerificationEmergencyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationEmergencyIdFragment(): VerificationEmergencyIdFragment
}

package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.landing.MobileFragment
import one.mixin.android.ui.landing.VerificationFragment

@Module
abstract class LandingActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeMobileFragment(): MobileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationFragment(): VerificationFragment
}

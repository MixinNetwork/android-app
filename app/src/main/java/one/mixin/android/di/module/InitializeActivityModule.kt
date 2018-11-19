package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.landing.LoadingFragment
import one.mixin.android.ui.landing.SetupNameFragment
import one.mixin.android.ui.landing.TimeFragment

@Module
abstract class InitializeActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeNameFragment(): SetupNameFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLoadingFragment(): LoadingFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTimeFragment(): TimeFragment
}
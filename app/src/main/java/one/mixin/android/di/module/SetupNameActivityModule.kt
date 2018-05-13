package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.landing.LoadingFragment
import one.mixin.android.ui.landing.SetupNameFragment

@Module
abstract class SetupNameActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeNameFragment(): SetupNameFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLoadingFragment(): LoadingFragment
}
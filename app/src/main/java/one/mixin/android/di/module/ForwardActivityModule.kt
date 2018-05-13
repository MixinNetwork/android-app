package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.forward.ForwardFragment

@Module
abstract class ForwardActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeForwardFragment(): ForwardFragment
}
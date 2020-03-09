package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.conversation.location.LocationFragment

@Module
abstract class LocationActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeLocationFragment(): LocationFragment
}

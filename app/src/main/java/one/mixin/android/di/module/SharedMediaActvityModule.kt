package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.media.AudioFragment
import one.mixin.android.ui.media.FileFragment
import one.mixin.android.ui.media.LinkFragment
import one.mixin.android.ui.media.MediaFragment
import one.mixin.android.ui.media.SharedMediaFragment

@Module
abstract class SharedMediaActvityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeSharedMediaFragment(): SharedMediaFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMediaFragment(): MediaFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAudioFragment(): AudioFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFileFragment(): FileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkFragment(): LinkFragment
}

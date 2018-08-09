package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.group.GroupFragment
import one.mixin.android.ui.group.GroupInfoFragment
import one.mixin.android.ui.group.NewGroupFragment

@Module
abstract class GroupActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeGroupFragment(): GroupFragment

    @ContributesAndroidInjector
    internal abstract fun contributeNewGroupFragment(): NewGroupFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupInfoFragment(): GroupInfoFragment
}

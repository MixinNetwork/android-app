package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.group.GroupEditFragment

@Module
abstract class UrlInterpreterActivityModule {

    @ContributesAndroidInjector
    internal abstract fun contributeAuthBottomSheetDialogFragment(): AuthBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupEditFragment(): GroupEditFragment
}

package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.setting.AboutFragment
import one.mixin.android.ui.setting.AuthenticationsFragment
import one.mixin.android.ui.setting.BackUpFragment
import one.mixin.android.ui.setting.NotificationsFragment
import one.mixin.android.ui.setting.SettingBlockedFragment
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.ui.setting.SettingFragment
import one.mixin.android.ui.setting.SettingStorageFragment

@Module
abstract class SettingActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeSettingFragment(): SettingFragment

    @ContributesAndroidInjector
    internal abstract fun contributeNotificationsFragment(): NotificationsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBlockedFragment(): SettingBlockedFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSettingStorageFragment(): SettingStorageFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSettingConversationFragment(): SettingConversationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAboutFragment(): AboutFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuthenticationsFragment(): AuthenticationsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBackUpFragment(): BackUpFragment
}
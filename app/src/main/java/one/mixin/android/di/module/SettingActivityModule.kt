package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.setting.AboutFragment
import one.mixin.android.ui.setting.AuthenticationsFragment
import one.mixin.android.ui.setting.BackUpFragment
import one.mixin.android.ui.setting.EmergencyContactTipBottomSheetDialogFragment
import one.mixin.android.ui.setting.FriendsNoBotFragment
import one.mixin.android.ui.setting.NotificationsFragment
import one.mixin.android.ui.setting.PrivacyFragment
import one.mixin.android.ui.setting.SettingBlockedFragment
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.ui.setting.SettingDataStorageFragment
import one.mixin.android.ui.setting.SettingFragment
import one.mixin.android.ui.setting.SettingStorageFragment
import one.mixin.android.ui.setting.VerificationEmergencyFragment

@Module
abstract class SettingActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeNotificationsFragment(): NotificationsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBlockedFragment(): SettingBlockedFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSettingStorageFragment(): SettingStorageFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSettingDataStorageFragment(): SettingDataStorageFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSettingConversationFragment(): SettingConversationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAboutFragment(): AboutFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuthenticationsFragment(): AuthenticationsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBackUpFragment(): BackUpFragment

    @ContributesAndroidInjector
    internal abstract fun contributeDeviceFragment(): DeviceFragment

    @ContributesAndroidInjector
    internal abstract fun contributeConfirmBottomFragment(): ConfirmBottomFragment

    @ContributesAndroidInjector
    internal abstract fun contributeEmergencyBottomFragment(): EmergencyContactTipBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerifyFragment(): VerifyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFriendsNoBotFragment(): FriendsNoBotFragment

    @ContributesAndroidInjector
    internal abstract fun contributePrivacyFragment(): PrivacyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationEmergencyFragment(): VerificationEmergencyFragment
}
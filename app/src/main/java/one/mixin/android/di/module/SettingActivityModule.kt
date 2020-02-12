package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.device.DeviceFragment
import one.mixin.android.ui.setting.AboutFragment
import one.mixin.android.ui.setting.AuthenticationsFragment
import one.mixin.android.ui.setting.BackUpFragment
import one.mixin.android.ui.setting.BiometricTimeFragment
import one.mixin.android.ui.setting.CurrencyBottomSheetDialogFragment
import one.mixin.android.ui.setting.EmergencyContactFragment
import one.mixin.android.ui.setting.EmergencyContactTipBottomSheetDialogFragment
import one.mixin.android.ui.setting.FriendsNoBotFragment
import one.mixin.android.ui.setting.MobileContactFragment
import one.mixin.android.ui.setting.NotificationsFragment
import one.mixin.android.ui.setting.OldPasswordFragment
import one.mixin.android.ui.setting.PermissionListFragment
import one.mixin.android.ui.setting.PinEmergencyBottomSheetDialog
import one.mixin.android.ui.setting.PinLogsFragment
import one.mixin.android.ui.setting.PrivacyFragment
import one.mixin.android.ui.setting.SettingBlockedFragment
import one.mixin.android.ui.setting.SettingConversationFragment
import one.mixin.android.ui.setting.SettingDataStorageFragment
import one.mixin.android.ui.setting.SettingStorageFragment
import one.mixin.android.ui.setting.VerificationEmergencyFragment
import one.mixin.android.ui.setting.ViewEmergencyContactFragment
import one.mixin.android.ui.setting.WalletSettingFragment
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment

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
    internal abstract fun contributeEmergencyBottomFragment(): EmergencyContactTipBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFriendsNoBotFragment(): FriendsNoBotFragment

    @ContributesAndroidInjector
    internal abstract fun contributePrivacyFragment(): PrivacyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerificationEmergencyFragment(): VerificationEmergencyFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinEmergencyBottomSheetDialog(): PinEmergencyBottomSheetDialog

    @ContributesAndroidInjector
    internal abstract fun contributeEmergencyContactFragment(): EmergencyContactFragment

    @ContributesAndroidInjector
    internal abstract fun contributeViewEmergencyContactFragment(): ViewEmergencyContactFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMobileContactFragment(): MobileContactFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletOldPasswordFragment(): OldPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletSettingFragment(): WalletSettingFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinLogsFragment(): PinLogsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBiometricTimeFragment(): BiometricTimeFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinBiometricsBottomSheetDialogFragment(): PinBiometricsBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeCurrencyBottomSheetDialogFragment(): CurrencyBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributePermissionListFragment(): PermissionListFragment
}

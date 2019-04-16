package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.wallet.AllTransactionsFragment
import one.mixin.android.ui.wallet.AssetAddFragment
import one.mixin.android.ui.wallet.BiometricTimeFragment
import one.mixin.android.ui.wallet.ContractBottomSheetDialogFragment
import one.mixin.android.ui.wallet.DepositQrBottomFragment
import one.mixin.android.ui.wallet.FeeFragment
import one.mixin.android.ui.wallet.HiddenAssetsFragment
import one.mixin.android.ui.wallet.OldPasswordFragment
import one.mixin.android.ui.wallet.PinBiometricsBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinCheckDialogFragment
import one.mixin.android.ui.wallet.SingleFriendSelectFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletFragment
import one.mixin.android.ui.wallet.WalletGetFreeFragment
import one.mixin.android.ui.wallet.WalletPasswordFragment
import one.mixin.android.ui.wallet.WalletSettingFragment
import one.mixin.android.ui.wallet.WithdrawalBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WithdrawalFragment

@Module
abstract class WalletActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeWalletFragment(): WalletFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletPasswordFragment(): WalletPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletTransactionsFragment(): TransactionsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletTransactionFragment(): TransactionFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletOldPasswordFragment(): OldPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletGetFreeFragment(): WalletGetFreeFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFeeFragment(): FeeFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinCheckDialogFragment(): PinCheckDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeHiddenAssetsFragment(): HiddenAssetsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWithdrawalFragment(): WithdrawalFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWithdrawalBottomSheetDialogFragment(): WithdrawalBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAllTransactionsFragment(): AllTransactionsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeDepositQrBottomFragment(): DepositQrBottomFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletSettingFragment(): WalletSettingFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinBiometricsBottomSheetDialogFragment(): PinBiometricsBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBiometricTimeFragment(): BiometricTimeFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSingleFriendSelectFragment(): SingleFriendSelectFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAssetAddFragment(): AssetAddFragment

    @ContributesAndroidInjector
    internal abstract fun contributeContractBottomSheetDialogFragment(): ContractBottomSheetDialogFragment
}
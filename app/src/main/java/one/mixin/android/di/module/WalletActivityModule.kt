package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.wallet.AddressAddFragment
import one.mixin.android.ui.wallet.AddressManagementFragment
import one.mixin.android.ui.wallet.AllTransactionsFragment
import one.mixin.android.ui.wallet.DepositQrBottomFragment
import one.mixin.android.ui.wallet.FeeFragment
import one.mixin.android.ui.wallet.HiddenAssetsFragment
import one.mixin.android.ui.wallet.OldPasswordFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.PinCheckDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletChangePasswordFragment
import one.mixin.android.ui.wallet.WalletFragment
import one.mixin.android.ui.wallet.WalletGetFreeFragment
import one.mixin.android.ui.wallet.WalletPasswordFragment
import one.mixin.android.ui.wallet.WithdrawalBottomSheetDialogFragment
import one.mixin.android.ui.wallet.WithdrawalFragment

@Module
abstract class WalletActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeWalletFragment(): WalletFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletPasswordFragment(): WalletPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletChangePasswordFragment(): WalletChangePasswordFragment

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
    internal abstract fun contributePinAddrBottomSheetFragment(): PinAddrBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAddressManagementFragment(): AddressManagementFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAddressAddFragment(): AddressAddFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAllTransactionsFragment(): AllTransactionsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeDepositQrBottomFragment(): DepositQrBottomFragment
}
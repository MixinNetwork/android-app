package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.EditBottomSheetDialogFragment
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.MultisigsBottomSheetDialogFragment
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.GiphyBottomSheetFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.UserTransactionsFragment
import one.mixin.android.ui.conversation.WithdrawalTipBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment

@Module
abstract class CommonModule {
    @ContributesAndroidInjector
    internal abstract fun contributeTransferBottomSheetDialogFragment(): TransferBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupBottomSheetFragment(): GroupBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserBottomSheetFragment(): UserBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkBottomSheetDialogFragment(): LinkBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWebBottomSheetDialogFragment(): WebBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferFragment(): TransferFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuthBottomSheetDialogFragment(): AuthBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserTransactionsFragment(): UserTransactionsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGiphyBottomSheetDialogFragment(): GiphyBottomSheetFragment

    @ContributesAndroidInjector
    internal abstract fun contributePermissionBottomSheetDialogFragment(): PermissionBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributePinAddrBottomSheetFragment(): PinAddrBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWithdrawalTipBottomSheetFragment(): WithdrawalTipBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeProfileFragment(): ProfileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletPasswordFragment(): WalletPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerifyFragment(): VerifyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeConfirmBottomFragment(): ConfirmBottomFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBiographyFragment(): EditBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSearchMessageItemFragment(): SearchMessageFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrScanBottomSheetDialogFragment(): QrScanBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMultisigsBottomSheetDialogFragment(): MultisigsBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserListBottomSheetDialogFragment(): UserListBottomSheetDialogFragment
}

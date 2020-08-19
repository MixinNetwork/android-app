package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.call.GroupUsersBottomSheetDialogFragment
import one.mixin.android.ui.common.AppListBottomSheetDialogFragment
import one.mixin.android.ui.common.CircleManagerFragment
import one.mixin.android.ui.common.EditDialog
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.JoinGroupBottomSheetDialogFragment
import one.mixin.android.ui.common.MultisigsBottomSheetDialogFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.UserListBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.common.profile.MySharedAppsFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.conversation.GiphyBottomSheetFragment
import one.mixin.android.ui.conversation.PreconditionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.UserTransactionsFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.PermissionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.home.bot.BotManagerBottomSheetDialogFragment
import one.mixin.android.ui.search.SearchMessageFragment
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.wallet.PinAddrBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.TransferOutViewFragment

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
    internal abstract fun contributePreconditionBottomSheetDialogFragment(): PreconditionBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletPasswordFragment(): WalletPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerifyFragment(): VerifyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeConfirmBottomFragment(): ConfirmBottomFragment

    @ContributesAndroidInjector
    internal abstract fun contributeEditDialog(): EditDialog

    @ContributesAndroidInjector
    internal abstract fun contributeSearchMessageItemFragment(): SearchMessageFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrScanBottomSheetDialogFragment(): QrScanBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMultisigsBottomSheetDialogFragment(): MultisigsBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserListBottomSheetDialogFragment(): UserListBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletTransactionFragment(): TransactionFragment

    @ContributesAndroidInjector
    internal abstract fun contributeProfileBottomSheetDialogFragment(): ProfileBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMySharedAppsFragment(): MySharedAppsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAppListBottomSheetDialogFragment(): AppListBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeCircleManagerFragment(): CircleManagerFragment

    @ContributesAndroidInjector
    internal abstract fun contributeBotManagerBottomSheetDialogFragment(): BotManagerBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeJoinGroupBottomSheetDialogFragment(): JoinGroupBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferOutViewFragment(): TransferOutViewFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransactionBottomSheetDialogFragment(): TransactionBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupUsersBottomSheetDialogFragment(): GroupUsersBottomSheetDialogFragment
}

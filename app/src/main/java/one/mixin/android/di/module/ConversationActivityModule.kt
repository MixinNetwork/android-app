package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.ConversationFragment
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.group.GroupEditFragment
import one.mixin.android.ui.group.GroupInfoFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.WalletPasswordFragment

@Module
abstract class ConversationActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeConversationFragment(): ConversationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferFragment(): TransferFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupInfoFragment(): GroupInfoFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkBottomSheetDialogFragment(): LinkBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWebBottomSheetDialogFragment(): WebBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuthBottomSheetDialogFragment(): AuthBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferBottenSheetDialogFragment(): TransferBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletPasswordFragment(): WalletPasswordFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerAlbumFragment(): StickerAlbumFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerFragment(): StickerFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWalletTransactionFragment(): TransactionFragment

    @ContributesAndroidInjector
    internal abstract fun contributeProfileFragment(): ProfileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserBottomSheetFragment(): UserBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupBottomSheetFragment(): GroupBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupEditFragment(): GroupEditFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrBottomSheetDialogFragment
}
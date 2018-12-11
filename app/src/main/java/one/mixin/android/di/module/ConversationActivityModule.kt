package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.conversation.ConversationFragment
import one.mixin.android.ui.conversation.FriendsFragment
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.ui.conversation.GiphyFragment
import one.mixin.android.ui.panel.PanelFragment
import one.mixin.android.ui.group.GroupEditFragment
import one.mixin.android.ui.group.GroupInfoFragment
import one.mixin.android.ui.sticker.StickerAddFragment
import one.mixin.android.ui.sticker.StickerManagementFragment
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.WalletPasswordFragment

@Module
abstract class ConversationActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeConversationFragment(): ConversationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupInfoFragment(): GroupInfoFragment

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
    internal abstract fun contributeGroupEditFragment(): GroupEditFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrScanBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFriendsFragment(): FriendsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerManagementFragment(): StickerManagementFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerAddFragment(): StickerAddFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGiphyFragment(): GiphyFragment

    @ContributesAndroidInjector
    internal abstract fun contributePanelFragment(): PanelFragment
}
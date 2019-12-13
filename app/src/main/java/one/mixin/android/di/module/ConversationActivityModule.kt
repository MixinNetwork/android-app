package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.conversation.ConversationFragment
import one.mixin.android.ui.conversation.FriendsFragment
import one.mixin.android.ui.conversation.GiphyFragment
import one.mixin.android.ui.conversation.MenuFragment
import one.mixin.android.ui.conversation.StickerAlbumFragment
import one.mixin.android.ui.conversation.StickerFragment
import one.mixin.android.ui.group.GroupInfoFragment
import one.mixin.android.ui.sticker.StickerAddFragment
import one.mixin.android.ui.sticker.StickerManagementFragment

@Module
abstract class ConversationActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeConversationFragment(): ConversationFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupInfoFragment(): GroupInfoFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerAlbumFragment(): StickerAlbumFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerFragment(): StickerFragment

    @ContributesAndroidInjector
    internal abstract fun contributeFriendsFragment(): FriendsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerManagementFragment(): StickerManagementFragment

    @ContributesAndroidInjector
    internal abstract fun contributeStickerAddFragment(): StickerAddFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGiphyFragment(): GiphyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeMenuFragment(): MenuFragment
}

package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.group.GroupEditFragment
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.search.SearchFragment

@Module
abstract class MainActivityModule {

    @ContributesAndroidInjector
    internal abstract fun contributeConversationListFragment(): ConversationListFragment

    @ContributesAndroidInjector
    internal abstract fun contributeSearchFragment(): SearchFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkBottomSheetDialogFragment(): LinkBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkWebBottomSheetDialogFragment(): WebBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAuthBottomSheetDialogFragment(): AuthBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferBottomSheetDialogFragment(): TransferBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupBottomSheetFragment(): GroupBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserBottomSheetFragment(): UserBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupEditFragment(): GroupEditFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrScanBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferFragment(): TransferFragment
}

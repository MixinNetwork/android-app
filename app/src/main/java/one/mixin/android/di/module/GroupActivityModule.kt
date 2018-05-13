package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.group.GroupFragment
import one.mixin.android.ui.group.GroupInfoFragment
import one.mixin.android.ui.group.NewGroupFragment

@Module
abstract class GroupActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeGroupFragment(): GroupFragment

    @ContributesAndroidInjector
    internal abstract fun contributeNewGroupFragment(): NewGroupFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupInfoFragment(): GroupInfoFragment

    @ContributesAndroidInjector
    internal abstract fun contributeLinkBottomSheetDialogFragment(): LinkBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWebBottomSheetDialogFragment(): WebBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeTransferBottomSheetDialogFragment(): TransferBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserBottomSheetFragment(): UserBottomSheetDialogFragment
}

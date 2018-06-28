package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.TransferFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment

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
}
package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.GroupBottomSheetDialogFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.contacts.AddPeopleFragment
import one.mixin.android.ui.contacts.ContactBottomSheetDialog
import one.mixin.android.ui.contacts.ContactsFragment
import one.mixin.android.ui.contacts.ProfileFragment
import one.mixin.android.ui.contacts.QRFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment

@Module
abstract class ContactActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeContactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeProfileFragment(): ProfileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQRFragment(): QRFragment

    @ContributesAndroidInjector
    internal abstract fun contributeContactBottomSheetDialogFragment(): ContactBottomSheetDialog

    @ContributesAndroidInjector
    internal abstract fun contributeAddPeopleFragment(): AddPeopleFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerifyFragment(): VerifyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeUserBottomSheetFragment(): UserBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeGroupBottomSheetDialogFragment(): GroupBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contributeWebBottomSheetDialogFragment(): WebBottomSheetDialogFragment
}
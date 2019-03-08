package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.QrBottomSheetDialogFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.ui.contacts.AddPeopleFragment
import one.mixin.android.ui.contacts.ContactBottomSheetDialog
import one.mixin.android.ui.contacts.ContactsFragment
import one.mixin.android.ui.contacts.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.contacts.ProfileFragment

@Module
abstract class ContactActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeContactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeProfileFragment(): ProfileFragment

    @ContributesAndroidInjector
    internal abstract fun contributeContactBottomSheetDialogFragment(): ContactBottomSheetDialog

    @ContributesAndroidInjector
    internal abstract fun contributeAddPeopleFragment(): AddPeopleFragment

    @ContributesAndroidInjector
    internal abstract fun contributeVerifyFragment(): VerifyFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrBottomSheetDialogFragment

    @ContributesAndroidInjector
    internal abstract fun contribteProfileBottomSheetDailogFragment(): ProfileBottomSheetDialogFragment
}
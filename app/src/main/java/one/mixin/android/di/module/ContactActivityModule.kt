package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.contacts.AddPeopleFragment
import one.mixin.android.ui.contacts.ContactBottomSheetDialog
import one.mixin.android.ui.contacts.ContactsFragment

@Module
abstract class ContactActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeContactsFragment(): ContactsFragment

    @ContributesAndroidInjector
    internal abstract fun contributeContactBottomSheetDialogFragment(): ContactBottomSheetDialog

    @ContributesAndroidInjector
    internal abstract fun contributeAddPeopleFragment(): AddPeopleFragment
}

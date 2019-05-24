package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.address.AddressAddFragment
import one.mixin.android.ui.address.AddressManagementFragment

@Module
abstract class AddressActivityModule {

    @ContributesAndroidInjector
    internal abstract fun contributeAddressManagementFragment(): AddressManagementFragment

    @ContributesAndroidInjector
    internal abstract fun contributeAddressAddFragment(): AddressAddFragment
}
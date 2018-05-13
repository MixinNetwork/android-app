package one.mixin.android.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.contacts.ContactViewModel
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.group.GroupViewModel
import one.mixin.android.ui.group.InviteViewModel
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.ui.landing.LoadingViewModel
import one.mixin.android.ui.landing.MobileViewModel
import one.mixin.android.ui.qr.CaptureViewModel
import one.mixin.android.ui.search.SearchViewModel
import one.mixin.android.ui.setting.SettingBlockedViewModel
import one.mixin.android.ui.setting.SettingConversationViewModel
import one.mixin.android.ui.setting.SettingPrivacyViewModel
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.wallet.PinCheckViewModel
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.viewmodel.ViewModelFactory

@Module
internal abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(ContactViewModel::class)
    internal abstract fun bindContactViewModel(contactViewModel: ContactViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConversationListViewModel::class)
    internal abstract fun bindConversationListViewModel(messageViewModel: ConversationListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WalletViewModel::class)
    internal abstract fun bindWalletViewModel(settingsViewModel: WalletViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConversationViewModel::class)
    internal abstract fun bindConversationViewModel(chatViewModel: ConversationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MobileViewModel::class)
    internal abstract fun bindMobileViewModel(mobileViewModel: MobileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    internal abstract fun bindSearchViewModel(settingsViewModel: SearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GroupViewModel::class)
    internal abstract fun bindGroupViewModel(groupViewModel: GroupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingViewModel::class)
    internal abstract fun bindSettingViewModel(settingsViewModel: SettingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingBlockedViewModel::class)
    internal abstract fun bindSettingBlockedViewModel(settingBlockedViewModel: SettingBlockedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingConversationViewModel::class)
    internal abstract fun bindSettingConversationViewModel(settingConversationViewModel: SettingConversationViewModel):
        ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingPrivacyViewModel::class)
    internal abstract fun bindSettingPrivacyViewModel(settingPrivacyViewModel: SettingPrivacyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BottomSheetViewModel::class)
    internal abstract fun bindBottomSheetViewModel(bottomSheetViewModel: BottomSheetViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(InviteViewModel::class)
    internal abstract fun bindInviteViewModel(inviteViewModel: InviteViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoadingViewModel::class)
    internal abstract fun bindLoadingViewModel(loadingViewModel: LoadingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CaptureViewModel::class)
    internal abstract fun bindCaptureViewModel(captureViewModel: CaptureViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PinCheckViewModel::class)
    internal abstract fun bindPinViewModel(pinCheckViewModel: PinCheckViewModel): ViewModel

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}

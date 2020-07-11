package one.mixin.android.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import one.mixin.android.ui.address.AddressViewModel
import one.mixin.android.ui.call.CallViewModel
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.profile.MySharedAppsViewModel
import one.mixin.android.ui.contacts.ContactViewModel
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.group.GroupViewModel
import one.mixin.android.ui.group.InviteViewModel
import one.mixin.android.ui.home.ConversationListViewModel
import one.mixin.android.ui.home.bot.BotManagerViewModel
import one.mixin.android.ui.landing.LoadingViewModel
import one.mixin.android.ui.landing.MobileViewModel
import one.mixin.android.ui.media.SharedMediaViewModel
import one.mixin.android.ui.search.SearchViewModel
import one.mixin.android.ui.setting.EmergencyViewModel
import one.mixin.android.ui.setting.SettingBlockedViewModel
import one.mixin.android.ui.setting.SettingConversationViewModel
import one.mixin.android.ui.setting.SettingStorageViewModel
import one.mixin.android.ui.setting.SettingViewModel
import one.mixin.android.ui.url.UrlInterpreterViewModel
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
    internal abstract fun bindWalletViewModel(walletViewModel: WalletViewModel): ViewModel

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
    internal abstract fun bindSearchViewModel(searchViewModel: SearchViewModel): ViewModel

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
    @ViewModelKey(SettingStorageViewModel::class)
    internal abstract fun bindSettingStorageViewModel(settingStorageViewModel: SettingStorageViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingConversationViewModel::class)
    internal abstract fun bindSettingConversationViewModel(settingConversationViewModel: SettingConversationViewModel):
        ViewModel

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
    @ViewModelKey(PinCheckViewModel::class)
    internal abstract fun bindPinViewModel(pinCheckViewModel: PinCheckViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddressViewModel::class)
    internal abstract fun bindAddressViewModel(addressViewModel: AddressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EmergencyViewModel::class)
    internal abstract fun bindEmergencyViewModel(emergencyViewModel: EmergencyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SharedMediaViewModel::class)
    internal abstract fun bindSharedViewModel(sharedMediaViewModel: SharedMediaViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UrlInterpreterViewModel::class)
    internal abstract fun bindUrlInterpreterModel(urlInterpreterViewModel: UrlInterpreterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MySharedAppsViewModel::class)
    internal abstract fun bindMySharedAppsViewModel(mySharedAppsViewModel: MySharedAppsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BotManagerViewModel::class)
    internal abstract fun bindBotManagerViewModel(botManagerViewModel: BotManagerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CallViewModel::class)
    internal abstract fun bindCallViewModel(callViewModel: CallViewModel): ViewModel

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}

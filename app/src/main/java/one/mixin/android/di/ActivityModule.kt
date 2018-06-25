package one.mixin.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.di.module.CommonModule
import one.mixin.android.di.module.CaptureActivityModule
import one.mixin.android.di.module.ContactActivityModule
import one.mixin.android.di.module.ConversationActivityModule
import one.mixin.android.di.module.ForwardActivityModule
import one.mixin.android.di.module.GroupActivityModule
import one.mixin.android.di.module.InviteActivityModule
import one.mixin.android.di.module.LandingActivityModule
import one.mixin.android.di.module.MainActivityModule
import one.mixin.android.di.module.SettingActivityModule
import one.mixin.android.di.module.SetupNameActivityModule
import one.mixin.android.di.module.UrlInterpreterActivityModule
import one.mixin.android.di.module.WalletActivityModule
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.media.DragMediaActivity
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.group.InviteActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.landing.SetupNameActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.ui.wallet.WalletActivity

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector(modules = [(CommonModule::class), (MainActivityModule::class)])
    internal abstract fun contributeMain(): MainActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (ConversationActivityModule::class)])
    internal abstract fun contributeConversation(): ConversationActivity

    @ContributesAndroidInjector(modules = [(LandingActivityModule::class)])
    internal abstract fun contributeLanding(): LandingActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (GroupActivityModule::class)])
    internal abstract fun contributeGroup(): GroupActivity

    @ContributesAndroidInjector(modules = [(SetupNameActivityModule::class)])
    internal abstract fun contributeSetupName(): SetupNameActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (ContactActivityModule::class)])
    internal abstract fun contributeContacts(): ContactsActivity

    @ContributesAndroidInjector(modules = [(WalletActivityModule::class)])
    internal abstract fun contributeWallet(): WalletActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (SettingActivityModule::class)])
    internal abstract fun contributeSetting(): SettingActivity

    @ContributesAndroidInjector(modules = [(ForwardActivityModule::class)])
    internal abstract fun contributeForward(): ForwardActivity

    @ContributesAndroidInjector(modules = [(InviteActivityModule::class)])
    internal abstract fun contributeInvite(): InviteActivity

    @ContributesAndroidInjector(modules = [(CaptureActivityModule::class)])
    internal abstract fun contributeCapture(): CaptureActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (ConversationActivityModule::class)])
    internal abstract fun contributeDragMedia(): DragMediaActivity

    @ContributesAndroidInjector(modules = [(CommonModule::class), (UrlInterpreterActivityModule::class)])
    internal abstract fun contributeUrlInterpreter(): UrlInterpreterActivity
}
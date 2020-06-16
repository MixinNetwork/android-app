package one.mixin.android.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.receiver.ExitBroadcastReceiver

@Module
abstract class BroadcastReceiverModule {
    @ContributesAndroidInjector abstract fun contributesExitBroadcastReceiver() : ExitBroadcastReceiver
}
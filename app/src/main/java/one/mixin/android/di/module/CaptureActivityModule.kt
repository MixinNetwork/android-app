package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.ui.qr.EditFragment
import one.mixin.android.ui.qr.ScanFragment

@Module
abstract class CaptureActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeCaptureFragment(): CaptureFragment

    @ContributesAndroidInjector
    internal abstract fun contributeScanFragment(): ScanFragment

    @ContributesAndroidInjector
    internal abstract fun contributeEditFragment(): EditFragment
}

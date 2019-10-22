package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.qr.CameraXCaptureFragment
import one.mixin.android.ui.qr.EditFragment

@Module
abstract class CaptureActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeCameraXCaptureFragment(): CameraXCaptureFragment

    @ContributesAndroidInjector
    internal abstract fun contributeEditFragment(): EditFragment
}

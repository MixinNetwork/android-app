package one.mixin.android.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.qr.CameraXCaptureFragment
import one.mixin.android.ui.qr.EditFragment
import one.mixin.android.ui.qr.ZxingCaptureFragment

@Module
abstract class CaptureActivityModule {
    @ContributesAndroidInjector
    internal abstract fun contributeCameraXCaptureFragment(): CameraXCaptureFragment

    @ContributesAndroidInjector
    internal abstract fun contributeZxingCaptureFragment(): ZxingCaptureFragment

    @ContributesAndroidInjector
    internal abstract fun contributeConfirmBottomFragment(): ConfirmBottomFragment

    @ContributesAndroidInjector
    internal abstract fun contributeEditFragment(): EditFragment

    @ContributesAndroidInjector
    internal abstract fun contributeQrBottomSheetDialogFragment(): QrScanBottomSheetDialogFragment
}
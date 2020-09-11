package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import kotlinx.android.synthetic.main.fragment_scan.*
import one.mixin.android.R
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.ConversationActivity.Companion.ARGS_SHORTCUT
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import org.jetbrains.anko.getStackTraceString
import timber.log.Timber

class ScanFragment : BaseCameraxFragment() {
    companion object {
        const val TAG = "ScanFragment"

        fun newInstance(
            forScanResult: Boolean = false,
            fromShortcut: Boolean = false
        ) = ScanFragment().withArgs {
            putBoolean(ARGS_FOR_SCAN_RESULT, forScanResult)
            putBoolean(ARGS_SHORTCUT, fromShortcut)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forScanResult = requireArguments().getBoolean(ARGS_FOR_SCAN_RESULT)
        fromShortcut = requireArguments().getBoolean(ARGS_SHORTCUT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_scan, container, false)

    @SuppressLint("RestrictedApi")
    override fun onFlashClick() {
        if (camera?.cameraInfo?.hasFlashUnit() == false) {
            toast(R.string.no_flash_unit)
            return
        }
        val torchState = camera?.cameraInfo?.torchState?.value ?: TorchState.OFF
        flash.setImageResource(R.drawable.ic_scan_flash)
        val future = (
            if (torchState == TorchState.ON) {
                camera?.cameraControl?.enableTorch(false)
            } else {
                camera?.cameraControl?.enableTorch(true)
            }
            ) ?: return
        Futures.addCallback(
            future,
            object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                }

                override fun onFailure(t: Throwable?) {
                    Timber.d("enableTorch onFailure, ${t?.getStackTraceString()}")
                }
            },
            mainExecutor
        )
    }

    @SuppressLint("RestrictedApi")
    override fun getOtherUseCases(
        rotation: Int
    ): Array<UseCase> {
        return arrayOf()
    }

    override fun onDisplayChanged(rotation: Int) {
    }

    override fun fromScan() = true
}

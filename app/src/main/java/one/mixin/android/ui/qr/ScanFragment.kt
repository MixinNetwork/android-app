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
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentScanBinding
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.ConversationActivity.Companion.ARGS_SHORTCUT
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.util.viewBinding
import timber.log.Timber

@AndroidEntryPoint
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

    private val binding by viewBinding(FragmentScanBinding::bind)

    override fun getContentView(): View = binding.root

    @SuppressLint("RestrictedApi")
    override fun onFlashClick() {
        if (camera?.cameraInfo?.hasFlashUnit() == false) {
            toast(R.string.Flash_unit_not_available)
            return
        }
        val torchState = camera?.cameraInfo?.torchState?.value ?: TorchState.OFF
        binding.flash.setImageResource(R.drawable.ic_scan_flash)
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
    override fun appendOtherUseCases(
        useCases: ArrayList<UseCase>,
        rotation: Int
    ) {}

    override fun onDisplayChanged(rotation: Int) {
    }

    override fun fromScan() = true
}

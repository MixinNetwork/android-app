package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import com.google.mlkit.vision.barcode.common.Barcode
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentScanBinding
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.bounce
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isDonateUrl
import one.mixin.android.extension.matchResourcePattern
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.job.RefreshExternalSchemeJob
import one.mixin.android.ui.conversation.ConversationActivity.Companion.ARGS_SHORTCUT
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_SCAN_RESULT
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.mlkit.scan.BaseCameraScanFragment
import one.mixin.android.util.mlkit.scan.analyze.AnalyzeResult
import one.mixin.android.util.mlkit.scan.analyze.Analyzer
import one.mixin.android.util.mlkit.scan.analyze.BarcodeResult
import one.mixin.android.util.mlkit.scan.analyze.BarcodeScanningAnalyzer
import one.mixin.android.util.mlkit.scan.camera.config.AspectRatioCameraConfig
import one.mixin.android.util.mlkit.scan.utils.PointUtils
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.ViewfinderView
import one.mixin.android.widget.gallery.ui.GalleryActivity
import timber.log.Timber

@AndroidEntryPoint
class ScanFragment : BaseCameraScanFragment<BarcodeResult>() {
    companion object {
        const val TAG = "ScanFragment"

        fun newInstance(
            forScanResult: Boolean = false,
            fromShortcut: Boolean = false,
        ) = ScanFragment().withArgs {
            putBoolean(ARGS_FOR_SCAN_RESULT, forScanResult)
            putBoolean(ARGS_SHORTCUT, fromShortcut)
        }
    }

    private val forScanResult by lazy { requireArguments().getBoolean(ARGS_FOR_SCAN_RESULT) }
    private val fromShortcut by lazy { requireArguments().getBoolean(ARGS_SHORTCUT) }

    override fun getLayoutId() = R.layout.fragment_scan

    override fun getPreviewViewId() = R.id.previewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.viewfinderView
                            .isShowPoints
                    ) {
                        binding.ivResult
                            .setImageBitmap(null)
                        binding.viewfinderView
                            .showScanner()
                        cameraScan.setAnalyzeImage(true)
                        return
                    } else {
                        activity?.finish()
                    }
                }
            },
        )
    }

    private val binding by viewBinding(FragmentScanBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            close.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            flash.setOnClickListener {
                cameraScan.enableTorch(cameraScan.isTorchEnabled().not())
                flash.bounce()
            }
            galleryIv.setOnClickListener {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    RxPermissions(requireActivity())
                        .request(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .autoDispose(stopScope)
                        .subscribe(
                            { granted ->
                                if (granted) {
                                    openGallery()
                                } else {
                                    context?.openPermissionSetting()
                                }
                            },
                            {
                            },
                        )
                } else {
                    openGallery()
                }
            }
        }
    }

    override fun createAnalyzer(): Analyzer<BarcodeResult> {
        return BarcodeScanningAnalyzer(Barcode.FORMAT_ALL_FORMATS)
    }

    override fun initCameraScan() {
        super.initCameraScan()
        cameraScan.setPlayBeep(false)
            .setCameraConfig(AspectRatioCameraConfig(requireContext()))
            .setVibrate(true)
    }

    override fun onScanResultCallback(result: AnalyzeResult<BarcodeResult>) {
        val width = result.bitmap?.width ?: return
        val height = result.bitmap?.height ?: return
        if (result.result?.content != null) {
            cameraScan.setAnalyzeImage(false)
            handleAnalysis(result.result?.content!!)
        } else if (result.result?.barcodes != null) {
            cameraScan.setAnalyzeImage(false)
            Timber.e("$width - $height ${binding.viewfinderView.width} ${binding.viewfinderView.height}")
            result.result?.barcodes?.let { results ->
                binding.ivResult.setImageBitmap(previewView.bitmap)
                val points = mutableListOf<Point>()
                for (barcode in results) {
                    barcode.boundingBox?.let { box ->
                        val point =
                            PointUtils.transform(
                                box.centerX(),
                                box.centerY(),
                                width,
                                height,
                                binding.viewfinderView.width,
                                binding.viewfinderView.height,
                            )
                        points.add(point)
                    }
                }
                Timber.e("$width - $height $points")
                binding.viewfinderView.showResultPoints(points)
                binding.viewfinderView.setOnItemClickListener(
                    object : ViewfinderView.OnItemClickListener {
                        override fun onItemClick(position: Int) {
                            handleAnalysis(results[position].displayValue!!)
                        }
                    },
                )
                if (points.size == 1) {
                    handleAnalysis(results[0].displayValue!!)
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                @Suppress("DEPRECATION")
                val path = it.getFilePath(MixinApplication.get())
                if (path == null) {
                    toast(R.string.File_error)
                } else {
                    if (data.hasExtra(GalleryActivity.IS_VIDEO)) {
                        openEdit(path, true, fromGallery = true)
                    } else {
                        openEdit(path, false, fromGallery = true)
                    }
                }
            }
        }
    }

    private fun openEdit(
        path: String,
        isVideo: Boolean,
        fromGallery: Boolean = false,
    ) {
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(path, isVideo, fromGallery, true), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    private fun handleAnalysis(analysisResult: String) {
        if (viewDestroyed()) return

        requireContext().heavyClickVibrate()
        requireContext().defaultSharedPreferences.putBoolean(CaptureActivity.SHOW_QR_CODE, false)
        if (forScanResult) {
            val scanResult =
                if (analysisResult.isDonateUrl()) {
                    val index = analysisResult.indexOf("?")
                    if (index != -1) {
                        analysisResult.take(index)
                    } else {
                        analysisResult
                    }
                } else {
                    analysisResult
                }
            val result =
                Intent().apply {
                    putExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT, scanResult)
                }
            activity?.setResult(Activity.RESULT_OK, result)
            activity?.finish()
            return
        }
        if (analysisResult.startsWith(Constants.Scheme.DEVICE)) {
            ConfirmBottomFragment.show(requireContext(), parentFragmentManager, analysisResult) { _, complete ->
                if (complete) {
                    activity?.finish()
                }
            }
            // } else if (analysisResult.startsWith(Constants.Scheme.DEVICE_TRANSFER)) {
            //     val uri = analysisResult.toUri()
            //     if (uri == Uri.EMPTY) {
            //         handleResult(requireActivity(), fromShortcut, analysisResult)
            //         return
            //     }
            //     TransferActivity.parseUri(requireContext(), false, uri, {
            //         activity?.finish()
            //     }) {
            //         handleResult(requireActivity(), fromShortcut, analysisResult)
            //     }
        } else {
            val externalSchemes =
                requireContext().defaultSharedPreferences.getStringSet(
                    RefreshExternalSchemeJob.PREF_EXTERNAL_SCHEMES,
                    emptySet(),
                )
            if (!externalSchemes.isNullOrEmpty() && analysisResult.matchResourcePattern(externalSchemes)) {
                WebActivity.show(requireContext(), analysisResult, null)
                activity?.finish()
                return
            }
            handleResult(requireActivity(), fromShortcut, analysisResult)
        }
    }
}

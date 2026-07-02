package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentScanBinding
import one.mixin.android.extension.bounce
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.matchResourcePattern
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
import one.mixin.android.util.mlkit.scan.analyze.BarcodeScanItem
import one.mixin.android.util.mlkit.scan.analyze.BarcodeResult
import one.mixin.android.util.mlkit.scan.analyze.BarcodeScanningAnalyzer
import one.mixin.android.util.mlkit.scan.analyze.ScanCoordinateMapper
import one.mixin.android.util.mlkit.scan.analyze.ScanDecision
import one.mixin.android.util.mlkit.scan.analyze.ScanResultSelector
import one.mixin.android.util.mlkit.scan.camera.config.AspectRatioCameraConfig
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.ViewfinderView

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
    private lateinit var getMediaResult: ActivityResultLauncher<PickVisualMediaRequest>
    private val scanResultSelector = ScanResultSelector()

    override fun getLayoutId() = R.layout.fragment_scan

    override fun getPreviewViewId() = R.id.previewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getMediaResult =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia(), ::onMediaPicked)
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
                        scanResultSelector.reset()
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
                getMediaResult.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
            .setVibrate(false)
    }

    override fun onScanResultCallback(result: AnalyzeResult<BarcodeResult>) {
        val items = result.result?.items.orEmpty()
        when (val decision = scanResultSelector.select(items)) {
            ScanDecision.Continue -> binding.viewfinderView.clearTrackedBounds()
            is ScanDecision.Track -> trackBarcode(decision.item)
            is ScanDecision.AutoHandle -> {
                cameraScan.setAnalyzeImage(false)
                binding.viewfinderView.clearTrackedBounds()
                handleAnalysis(decision.text)
            }
            is ScanDecision.ShowChoices -> showBarcodeChoices(decision.items)
        }
    }

    override fun onScanResultFailure() {
        if (!binding.viewfinderView.isShowPoints) {
            scanResultSelector.reset()
            binding.viewfinderView.clearTrackedBounds()
        }
    }

    private fun trackBarcode(item: BarcodeScanItem) {
        val box = item.boundingBox
        if (box == null) {
            binding.viewfinderView.clearTrackedBounds()
            return
        }
        binding.viewfinderView.trackResultBounds(
            ScanCoordinateMapper.transform(
                box,
                item.sourceWidth,
                item.sourceHeight,
                binding.viewfinderView.width,
                binding.viewfinderView.height,
            ),
        )
    }

    private fun showBarcodeChoices(items: List<BarcodeScanItem>) {
        cameraScan.setAnalyzeImage(false)
        binding.viewfinderView.clearTrackedBounds()
        binding.ivResult.setImageBitmap(previewView.bitmap)
        val choiceItems = mutableListOf<BarcodeScanItem>()
        val points = mutableListOf<Point>()
        for (item in items) {
            val box = item.boundingBox ?: continue
            points.add(
                ScanCoordinateMapper.transformCenter(
                    box,
                    item.sourceWidth,
                    item.sourceHeight,
                    binding.viewfinderView.width,
                    binding.viewfinderView.height,
                ),
            )
            choiceItems.add(item)
        }
        if (points.isEmpty()) {
            handleAnalysis(items.first().text)
            return
        }
        binding.viewfinderView.showResultPoints(points)
        binding.viewfinderView.setOnItemClickListener(
            object : ViewfinderView.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    handleAnalysis(choiceItems[position].text)
                }
            },
        )
    }

    private fun onMediaPicked(uri: Uri?) {
        uri?.let {
            @Suppress("DEPRECATION")
            val path = it.getFilePath(MixinApplication.get())
            if (path == null) {
                toast(R.string.File_error)
            } else {
                openEdit(path, false, fromGallery = true)
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
            val result =
                Intent().apply {
                    putExtra(ARGS_FOR_SCAN_RESULT, analysisResult)
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

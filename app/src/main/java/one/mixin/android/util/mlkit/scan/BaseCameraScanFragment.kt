package one.mixin.android.util.mlkit.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.camera.view.PreviewView
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.mlkit.scan.analyze.Analyzer
import one.mixin.android.util.mlkit.scan.camera.CameraScan

abstract class BaseCameraScanFragment<T> : BaseFragment(), CameraScan.OnScanResultCallback<T> {

    protected lateinit var rootView: View

    protected lateinit var previewView: PreviewView

    protected lateinit var flashlight: View

    protected lateinit var cameraScan: CameraScan<T>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (isContentView()) {
            rootView = createRootView(inflater, container)
        }
        initUi()
        initCameraScan()
        startCamera()
        return rootView
    }

    private fun initUi() {
        previewView = requireNotNull(rootView.findViewById(getPreviewViewId()))
    }

    open fun initCameraScan() {
        cameraScan = createCameraScan(previewView).setAnalyzer(createAnalyzer()).setOnScanResultCallback(this@BaseCameraScanFragment)
    }

    fun startCamera() {
        cameraScan.startCamera()
    }

    override fun onDestroy() {
        cameraScan.release()
        super.onDestroy()
    }

    open fun createRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(getLayoutId(), container, false)
    }

    open fun getLayoutId(): Int {
        return R.layout.view_ml_preview
    }

    @IdRes
    open fun getPreviewViewId() = R.id.preview_view

    private fun isContentView(): Boolean {
        return true
    }

    private fun createCameraScan(preview: PreviewView): CameraScan<T> = BaseCameraScan(this, preview)

    abstract fun createAnalyzer(): Analyzer<T>
}

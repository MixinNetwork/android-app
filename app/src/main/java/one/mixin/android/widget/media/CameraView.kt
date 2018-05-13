package one.mixin.android.widget.media

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.Parameters
import android.hardware.Camera.Size
import android.os.AsyncTask
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.OrientationEventListener
import android.view.ViewGroup
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.putInt
import org.whispersystems.libsignal.util.guava.Optional
import java.io.IOException
import java.util.Collections
import java.util.LinkedList

@Suppress("DEPRECATION")
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private val surface: CameraSurfaceView
    private val onOrientationChange: OnOrientationChange

    @Volatile
    private var camera = Optional.absent<Camera>()
    @Volatile
    private var cameraId = CameraInfo.CAMERA_FACING_BACK
    @Volatile
    private var displayOrientation = -1

    private var state = State.PAUSED
    private var previewSize: Size? = null
    private val listeners = Collections.synchronizedList(LinkedList<CameraViewListener>())
    private var outputOrientation = -1

    val isStarted: Boolean
        get() = state != State.PAUSED

    val isMultiCamera: Boolean
        get() = Camera.getNumberOfCameras() > 1

    val isRearCamera: Boolean
        get() = cameraId == CameraInfo.CAMERA_FACING_BACK

    private val cameraPictureOrientation: Int
        get() {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                outputOrientation = getCameraPictureRotation(activity.windowManager
                    .defaultDisplay
                    .orientation)
            } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                outputOrientation = (360 - displayOrientation) % 360
            } else {
                outputOrientation = displayOrientation
            }

            return outputOrientation
        }

    // https://github.com/WhisperSystems/Signal-Android/issues/4715
    private val isTroublemaker: Boolean
        get() = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT &&
            "JWR66Y" == Build.DISPLAY &&
            "yakju" == Build.PRODUCT

    private val cameraInfo: CameraInfo
        get() {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, info)
            return info
        }

    // XXX this sucks
    private val activity: Activity
        get() = context as Activity

    init {
        setBackgroundColor(Color.BLACK)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraView)
            val camera = typedArray.getInt(R.styleable.CameraView_camera, -1)
            if (camera != -1)
                cameraId = camera
            else if (isMultiCamera)
                cameraId = context.defaultSharedPreferences.getInt(TAG, cameraId)
            typedArray.recycle()
        }

        surface = CameraSurfaceView(getContext())
        onOrientationChange = OnOrientationChange(context.applicationContext)
        addView(surface)
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun onResume() {
        if (state != State.PAUSED) return
        state = State.RESUMED
        enqueueTask(object : SerialAsyncTask<Void>() {
            override fun onRunBackground(): Void? {
                try {
                    val openStartMillis = System.currentTimeMillis()
                    camera = Optional.fromNullable(Camera.open(cameraId))
                    synchronized(this@CameraView) {
                        //                        this@CameraView.notifyAll()
                    }
                    if (camera.isPresent) onCameraReady(camera.get())
                } catch (e: Exception) {
                }

                return null
            }

            override fun onPostMain(avoid: Void?) {
                if (!camera.isPresent) {
                    for (listener in listeners) {
                        listener.onCameraFail()
                    }
                    return
                }

                if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    onOrientationChange.enable()
                }
            }
        })
    }

    fun onPause() {
        if (state == State.PAUSED) return
        state = State.PAUSED

        enqueueTask(object : SerialAsyncTask<Void>() {
            private var cameraToDestroy: Optional<Camera>? = null

            override fun onPreMain() {
                cameraToDestroy = camera
                camera = Optional.absent()
            }

            override fun onRunBackground(): Void? {
                if (cameraToDestroy!!.isPresent) {
                    try {
                        stopPreview()
                        cameraToDestroy!!.get().setPreviewCallback(null)
                        cameraToDestroy!!.get().release()
                    } catch (e: Exception) {
                    }
                }
                return null
            }

            override fun onPostMain(avoid: Void?) {
                onOrientationChange.disable()
                displayOrientation = -1
                outputOrientation = -1
                removeView(surface)
                addView(surface)
            }
        })

        for (listener in listeners) {
            listener.onCameraStop()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val previewWidth: Int
        val previewHeight: Int

        if (camera.isPresent && previewSize != null) {
            if (displayOrientation == 90 || displayOrientation == 270) {
                previewWidth = previewSize!!.height
                previewHeight = previewSize!!.width
            } else {
                previewWidth = previewSize!!.width
                previewHeight = previewSize!!.height
            }
        } else {
            previewWidth = width
            previewHeight = height
        }

        if (previewHeight == 0 || previewWidth == 0) {
            return
        }

        if (width * previewHeight > height * previewWidth) {
            val scaledChildHeight = previewHeight * width / previewWidth
            surface.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2)
        } else {
            val scaledChildWidth = previewWidth * height / previewHeight
            surface.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (camera.isPresent) startPreview(camera.get().parameters)
    }

    fun addListener(listener: CameraViewListener) {
        listeners.add(listener)
    }

    fun setPreviewCallback(previewCallback: PreviewCallback) {
        enqueueTask(object : PostInitializationTask<Void>() {
            override fun onPostMain(avoid: Void?) {
                if (camera.isPresent) {
                    camera.get().setPreviewCallback(Camera.PreviewCallback { data, camera ->
                        if (!this@CameraView.camera.isPresent) {
                            return@PreviewCallback
                        }

                        val rotation = cameraPictureOrientation
                        val previewSize = camera.parameters.previewSize
                        if (data != null) {
                            previewCallback.onPreviewFrame(
                                PreviewFrame(data, previewSize.width, previewSize.height, rotation))
                        }
                    })
                }
            }
        })
    }

    fun flipCamera() {
        if (Camera.getNumberOfCameras() > 1) {
            cameraId = if (cameraId == CameraInfo.CAMERA_FACING_BACK)
                CameraInfo.CAMERA_FACING_FRONT
            else CameraInfo.CAMERA_FACING_BACK
            onPause()
            onResume()
            context.defaultSharedPreferences.putInt(TAG, cameraId)
        }
    }

    private fun onCameraReady(camera: Camera) {
        val parameters = camera.parameters

        parameters.setRecordingHint(true)
        val focusModes = parameters.supportedFocusModes
        if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        } else if (focusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }

        displayOrientation = CameraUtils.getCameraDisplayOrientation(activity, cameraInfo)
        camera.setDisplayOrientation(displayOrientation)
        camera.parameters = parameters
        enqueueTask(object : PostInitializationTask<Void>() {
            override fun onRunBackground(): Void? {
                try {
                    camera.setPreviewDisplay(surface.holder)
                    startPreview(parameters)
                } catch (e: Exception) {
                }

                return null
            }
        })
    }

    private fun startPreview(parameters: Parameters) {
        if (this.camera.isPresent) {
            try {
                val camera = this.camera.get()
                val preferredPreviewSize = getPreferredPreviewSize(parameters)

                if (preferredPreviewSize != null && parameters.previewSize != preferredPreviewSize) {
                    if (state == State.ACTIVE) stopPreview()
                    previewSize = preferredPreviewSize
                    parameters.setPreviewSize(preferredPreviewSize.width, preferredPreviewSize.height)
                    camera.parameters = parameters
                } else {
                    previewSize = parameters.previewSize
                }
                val previewStartMillis = System.currentTimeMillis()
                camera.startPreview()
                state = State.ACTIVE
                context.mainThread {
                    requestLayout()
                    for (listener in listeners) {
                        listener.onCameraStart()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun stopPreview() {
        if (camera.isPresent) {
            try {
                camera.get().stopPreview()
                state = State.RESUMED
            } catch (e: Exception) {
            }
        }
    }

    private fun getPreferredPreviewSize(parameters: Parameters): Size? {
        return CameraUtils.getPreferredPreviewSize(displayOrientation,
            measuredWidth,
            measuredHeight,
            parameters)
    }

    fun getCameraPictureRotation(orientation: Int): Int {
        var orientation = orientation
        val info = cameraInfo
        val rotation: Int

        orientation = (orientation + 45) / 90 * 90

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360
        } else {
            rotation = (info.orientation + orientation) % 360
        }

        return rotation
    }

    private inner class OnOrientationChange(context: Context) : OrientationEventListener(context) {
        init {
            disable()
        }

        override fun onOrientationChanged(orientation: Int) {
            if (camera.isPresent && orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                val newOutputOrientation = getCameraPictureRotation(orientation)

                if (newOutputOrientation != outputOrientation) {
                    outputOrientation = newOutputOrientation

                    val params = camera.get().parameters

                    params.setRotation(outputOrientation)

                    try {
                        camera.get().parameters = params
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    fun takePicture(previewRect: Rect) {
        if (!camera.isPresent || camera.get().parameters == null) {
            return
        }

        camera.get().setOneShotPreviewCallback { data, camera ->
            val rotation = cameraPictureOrientation
            val previewSize = camera.parameters.previewSize
            val croppingRect = getCroppedRect(previewSize, previewRect, rotation)

            CaptureTask(previewSize, rotation, croppingRect).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data)
        }
    }

    private fun getCroppedRect(cameraPreviewSize: Size, visibleRect: Rect, rotation: Int): Rect {
        val previewWidth = cameraPreviewSize.width
        val previewHeight = cameraPreviewSize.height

        if (rotation % 180 > 0) rotateRect(visibleRect)

        var scale = previewWidth.toFloat() / visibleRect.width()
        if (visibleRect.height() * scale > previewHeight) {
            scale = previewHeight.toFloat() / visibleRect.height()
        }
        val newWidth = visibleRect.width() * scale
        val newHeight = visibleRect.height() * scale
        val centerX: Int = if (isTroublemaker) {
            (previewWidth - newWidth / 2).toInt()
        } else {
            previewWidth / 2
        }
        val centerY = (previewHeight / 2).toFloat()

        visibleRect.set((centerX - newWidth / 2).toInt(),
            (centerY - newHeight / 2).toInt(),
            (centerX + newWidth / 2).toInt(),
            (centerY + newHeight / 2).toInt())

        if (rotation % 180 > 0) rotateRect(visibleRect)
        return visibleRect
    }

    private fun rotateRect(rect: Rect) {
        rect.set(rect.top, rect.left, rect.bottom, rect.right)
    }

    private abstract inner class PostInitializationTask<Result> : SerialAsyncTask<Result>() {
        @Throws(PreconditionsNotMetException::class)
        override fun onWait() {
            synchronized(this@CameraView) {
                if (!camera.isPresent) {
                    throw PreconditionsNotMetException()
                }
                while (measuredHeight <= 0 || measuredWidth <= 0 || !surface.isReady) {
                }
            }
        }
    }

    private inner class CaptureTask(
        private val previewSize: Size,
        private val rotation: Int,
        private val croppingRect: Rect
    ) : AsyncTask<ByteArray, Void, ByteArray>() {

        override fun doInBackground(vararg params: ByteArray): ByteArray? {
            val data = params[0]
            try {
                return BitmapUtil.createFromNV21(data,
                    previewSize.width,
                    previewSize.height,
                    rotation,
                    croppingRect,
                    cameraId == CameraInfo.CAMERA_FACING_FRONT)
            } catch (e: IOException) {
                return null
            }
        }

        override fun onPostExecute(imageBytes: ByteArray?) {
            if (imageBytes != null) {
                for (listener in listeners) {
                    listener.onImageCapture(imageBytes)
                }
            }
        }
    }

    private fun enqueueTask(job: SerialAsyncTask<*>) {
        MixinApplication.get().jobManager.addJobInBackground(job)
    }

    private abstract class SerialAsyncTask<Result> : Job(Params(0)) {

        override fun onAdded() {}

        override fun onRun() {
            try {
                onWait()
                applicationContext.mainThread {
                    onPreMain()
                }

                val result = onRunBackground()

                applicationContext.mainThread {
                    onPostMain(result)
                }
            } catch (e: PreconditionsNotMetException) {
                Log.w(TAG, "skipping task, preconditions not met in onWait()")
            }
        }

        override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        }

        override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint? =
            null

        @Throws(PreconditionsNotMetException::class)
        protected open fun onWait() {
        }

        protected open fun onPreMain() {}

        protected open fun onRunBackground(): Result? = null

        protected open fun onPostMain(result: Result?) {}
    }

    private class PreconditionsNotMetException : Exception()

    interface CameraViewListener {
        fun onImageCapture(imageBytes: ByteArray)

        fun onCameraFail()

        fun onCameraStart()

        fun onCameraStop()
    }

    interface PreviewCallback {
        fun onPreviewFrame(frame: PreviewFrame)
    }

    class PreviewFrame constructor(val data: ByteArray, val width: Int, val height: Int, val orientation: Int)

    private enum class State {
        PAUSED, RESUMED, ACTIVE
    }

    companion object {
        private val TAG = CameraView::class.java.simpleName
    }
}

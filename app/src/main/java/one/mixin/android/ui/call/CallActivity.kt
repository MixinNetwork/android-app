package one.mixin.android.ui.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.view_call_button.view.*
import one.mixin.android.R
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.fastBlur
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.vo.CallState
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.webrtc.CallService
import one.mixin.android.widget.CallButton
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class CallActivity : BaseActivity(), CallService.CallServiceCallback {

    private var bound = false
    private var disposable: Disposable? = null

    private var videoEnable = false
    private var eglBase: EglBase? = null
    private val localSink = ProxyVideoSink()
    private val remoteSink = ProxyVideoSink()
    private var swapFeed = false
    private var isFrontCamera = true

    @Inject
    lateinit var callState: CallState

    private var callService: CallService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            callService = (service as CallService.CallBinder).getService()
            callService!!.callback = this@CallActivity
            bound = true
            if (videoEnable) {
                setVideo()
            }
        }
    }

    inner class ProxyVideoSink : VideoSink {
        private var target: VideoSink? = null

        @Synchronized
        override fun onFrame(frame: VideoFrame?) {
            target?.onFrame(frame)
        }

        @Synchronized
        fun setTarget(sink: VideoSink?) {
            target = sink
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val answer = intent.getParcelableExtra<User?>(ARGS_ANSWER)
        if (answer != null) {
            name_tv.text = answer.fullName
            avatar.setInfo(answer.fullName, answer.avatarUrl, answer.identityNumber)
            avatar.setTextSize(22f)
            if (answer.avatarUrl != null) {
                setBlurBg(answer.avatarUrl)
            }
        }
        hangup_cb.setOnClickListener {
            handleHangup()
            handleDisconnected()
        }
        answer_cb.setOnClickListener {
            handleAnswer()
        }
        mute_cb.setOnCheckedChangeListener(object : CallButton.OnCheckedChangeListener {
            override fun onCheckedChanged(id: Int, checked: Boolean) {
                CallService.startService(this@CallActivity, CallService.ACTION_MUTE_AUDIO) {
                    it.putExtra(CallService.EXTRA_MUTE, checked)
                }
            }
        })
        voice_cb.setOnCheckedChangeListener(object : CallButton.OnCheckedChangeListener {
            override fun onCheckedChanged(id: Int, checked: Boolean) {
                CallService.startService(this@CallActivity, CallService.ACTION_SPEAKERPHONE) {
                    it.putExtra(CallService.EXTRA_SPEAKERPHONE, checked)
                }
            }
        })

        callState.observe(this, Observer { callInfo ->
            when (callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    if (callInfo.dialingStatus != MessageStatus.READ) {
                        handleDialingConnecting()
                    } else {
                        handleDialingWaiting()
                    }
                }
                CallService.CallState.STATE_RINGING -> {
                    handleRinging()
                }
                CallService.CallState.STATE_ANSWERING -> {
                    handleAnswering()
                }
                CallService.CallState.STATE_CONNECTED -> {
                    handleConnected()
                }
                CallService.CallState.STATE_BUSY -> {
                    handleBusy()
                }
                CallService.CallState.STATE_IDLE -> {
                    handleDisconnected()
                }
            }
        })

        videoEnable = intent.getBooleanExtra(ARGS_VIDEO, false)
        if (videoEnable) {
            initVideo()
        }

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT;
    }

    override fun onStart() {
        super.onStart()
        Intent(this, CallService::class.java).run {
            bindService(this, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
        action_tv?.removeCallbacks(timeRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()

        disposable?.dispose()
        eglBase?.release()
        local_render?.release()
        remote_render?.release()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (callState.isIdle()) {
            handleHangup()
        }
        handleDisconnected()
    }

    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
        runOnUiThread {
            this@CallActivity.isFrontCamera = isFrontCamera
            local_render.setMirror(isFrontCamera)
        }
    }

    private fun handleHangup() {
        when (callState.callInfo.callState) {
            CallService.CallState.STATE_DIALING -> CallService.startService(this, CallService.ACTION_CALL_CANCEL)
            CallService.CallState.STATE_RINGING, CallService.CallState.STATE_ANSWERING -> CallService.startService(this, CallService.ACTION_CALL_DECLINE)
            CallService.CallState.STATE_CONNECTED -> CallService.startService(this, CallService.ACTION_CALL_LOCAL_END)
            else -> CallService.startService(this, CallService.ACTION_CALL_CANCEL)
        }
    }

    private fun setBlurBg(url: String) {
        doAsync {
            try {
                val bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS)
                uiThread {
                    call_cl.background = BitmapDrawable(resources, bitmap.fastBlur(1f, 10))
                }
            } catch (timeoutException: TimeoutException) {
            }
        }
    }

    private fun initVideo() {
        remote_render.setOnClickListener {
            //TODO toggle control
        }
        local_render.setOnClickListener { swapFeed(!swapFeed) }
        eglBase = EglBase.create()
        remote_render.init(eglBase!!.eglBaseContext, null)
        remote_render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        remote_render.setEnableHardwareScaler(false)
        local_render.init(eglBase!!.eglBaseContext, null)
        local_render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        local_render.setEnableHardwareScaler(true)
        local_render.setMirror(true)
        local_render.setZOrderMediaOverlay(true)
        swapFeed(true)
    }

    private fun setVideo() {
        if (callService == null) return

        val videoCapturer = createVideoCapturer()
        callService!!.setVideo(eglBase!!, videoCapturer, localSink, remoteSink)
    }

    private fun swapFeed(swap: Boolean) {
        swapFeed = swap
        localSink.setTarget(if (swap) remote_render else local_render)
        remoteSink.setTarget(if (swap) local_render else remote_render)
        remote_render.setMirror(swap)
        local_render.setMirror(!swap)
    }

    private fun useCamera2(): Boolean {
        return Camera2Enumerator.isSupported(this)
    }

    private fun captureToTexture(): Boolean {
        return true
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // First, try to find front facing camera
        Timber.d("Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Timber.d("Creating front facing camera capturer.")
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Timber.d("Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Timber.d("Creating other camera capturer.")
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun createVideoCapturer(): VideoCapturer? {
        var videoCapturer: VideoCapturer? = null
        if (useCamera2()) {
            Timber.d("Creating capturer using camera2 API.")
            videoCapturer = createCameraCapturer(Camera2Enumerator(this))
        } else {
            Timber.d("Creating capturer using camera1 API.")
            videoCapturer = createCameraCapturer(Camera1Enumerator(captureToTexture()))
        }
        if (videoCapturer == null) {
            Timber.e("Failed to open camera")
            return null
        }
        return videoCapturer
    }

    @SuppressLint("CheckResult")
    private fun handleAnswer() {
        val rx = RxPermissions(this)
        if (videoEnable) {
            rx.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            rx.request(Manifest.permission.RECORD_AUDIO)
        }.subscribe { granted ->
            if (granted) {
                handleAnswering()
                CallService.startService(this@CallActivity, CallService.ACTION_CALL_ANSWER)
            } else {
                CallService.startService(this, CallService.ACTION_CALL_CANCEL)
                handleDisconnected()
            }
        }
    }

    private fun handleDialingConnecting() {
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.visibility = INVISIBLE
        moveHangup(true, 0)
        action_tv.text = getString(R.string.call_notification_outgoing)
    }

    private fun handleDialingWaiting() {
        if (voice_cb.visibility != INVISIBLE) {
            voice_cb.visibility = INVISIBLE
        }
        if (mute_cb.visibility != INVISIBLE) {
            mute_cb.visibility = INVISIBLE
        }
        if (answer_cb.visibility != INVISIBLE) {
            answer_cb.visibility = INVISIBLE
        }
        moveHangup(true, 0)
        action_tv.text = getString(R.string.call_notification_ringing)
    }

    private fun handleRinging() {
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.visibility = VISIBLE
        answer_cb.text.text = getString(R.string.call_accept)
        hangup_cb.text.text = getString(R.string.call_decline)
        moveHangup(false, 0)
        action_tv.text = getString(R.string.call_notification_incoming_voice)
    }

    private fun handleAnswering() {
        voice_cb.fadeIn()
        mute_cb.fadeIn()
        answer_cb.fadeOut()
        moveHangup(true, 250)
        action_tv.text = getString(R.string.call_connecting)
    }

    private fun handleConnected() {
        if (voice_cb.visibility == INVISIBLE) {
            voice_cb.fadeIn()
        }
        if (mute_cb.visibility == INVISIBLE) {
            mute_cb.fadeIn()
        }
        answer_cb.fadeOut()
        moveHangup(true, 250)
        action_tv.postDelayed(timeRunnable, 1000)
    }

    private fun handleDisconnected() {
        localSink.setTarget(null)
        remoteSink.setTarget(null)
        finish()
    }

    private fun handleBusy() {
        handleDisconnected()
    }

    private fun moveHangup(center: Boolean, duration: Long) {
        hangup_cb.visibility = VISIBLE
        val constraintSet = ConstraintSet().apply {
            clone(call_cl)
            setHorizontalBias(hangup_cb.id, if (center) 0.5f else 0.0f)
        }
        val transition = AutoTransition().apply {
            this.duration = duration
        }
        TransitionManager.beginDelayedTransition(call_cl, transition)
        constraintSet.applyTo(call_cl)
    }

    private val timeRunnable: Runnable by lazy {
        Runnable {
            if (callState.callInfo.connectedTime != null) {
                val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
                action_tv.text = duration.formatMillis()
            }
            action_tv.postDelayed(timeRunnable, 1000)
        }
    }

    companion object {
        const val TAG = "CallActivity"

        const val ARGS_ANSWER = "answer"
        const val ARGS_VIDEO = "video"

        fun show(context: Context, answer: User? = null, video: Boolean = false) {
            Intent(context, CallActivity::class.java).apply {
                putExtra(ARGS_ANSWER, answer)
                putExtra(ARGS_VIDEO, video)
            }.run {
                context.startActivity(this)
            }
        }
    }
}
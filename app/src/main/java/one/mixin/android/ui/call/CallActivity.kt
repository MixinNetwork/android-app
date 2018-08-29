package one.mixin.android.ui.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_call.*
import one.mixin.android.R
import one.mixin.android.extension.formatMillis
import one.mixin.android.vo.User
import one.mixin.android.webrtc.CallService
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import timber.log.Timber

class CallActivity : AppCompatActivity(), CallService.CallServiceCallback {

    private var bound = false
    private var disposable: Disposable? = null
    private var connectedTime = 0

    private var videoEnable = false
    private var eglBase: EglBase? = null
    private val localSink = ProxyVideoSink()
    private val remoteSink = ProxyVideoSink()
    private var swapFeed = false
    private var isFrontCamera = true

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
        val answer = intent.getParcelableExtra<User?>(ARGS_ANSWER)
        if (answer != null) {
            name_tv.text = answer.fullName
            avatar.setInfo(answer.fullName, answer.avatarUrl, answer.identityNumber)
        }
        hangup_fab.setOnClickListener {
            if (callService != null) {
                when (callService!!.callState) {
                    CallService.CallState.STATE_DIALING -> CallService.startService(this, CallService.ACTION_CALL_CANCEL)
                    CallService.CallState.STATE_RINGING -> CallService.startService(this, CallService.ACTION_CALL_DECLINE)
                    CallService.CallState.STATE_CONNECTED -> CallService.startService(this, CallService.ACTION_CALL_LOCAL_END)
                    else -> CallService.startService(this, CallService.ACTION_CALL_CANCEL)
                }
            }
            handleDisconnected()
        }
        answer_fab.setOnClickListener {
            handleAnswer()
        }
        mute_tb.setOnCheckedChangeListener { _, isChecked ->
            CallService.startService(this, CallService.ACTION_MUTE_AUDIO) {
                it.putExtra(CallService.EXTRA_MUTE, isChecked)
            }
        }
        front_tb.setOnClickListener {
            CallService.startService(this, CallService.ACTION_SWITCH_CAMERA)
        }

        videoEnable = intent.getBooleanExtra(ARGS_VIDEO, false)
        if (videoEnable) {
            initVideo()
        }

        volumeControlStream = AudioManager.STREAM_VOICE_CALL
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAction()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, CallService::class.java).run {
            bindService(this, connection, Context.BIND_AUTO_CREATE)
        }
        handleAction()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (callService != null && callService!!.callState != CallService.CallState.STATE_CONNECTED) {
            CallService.stopService(this)
        }
        disposable?.dispose()
        eglBase?.release()
        local_render?.release()
        remote_render?.release()
    }

    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
        runOnUiThread {
            this@CallActivity.isFrontCamera = isFrontCamera
            local_render.setMirror(isFrontCamera)
        }
    }

    private fun handleAction() {
        when (intent.action) {
            CallAction.CALL_OUTGOING.name -> {
                handleOutgoing()
            }
            CallAction.CALL_INCOMING.name -> {
                handleIncoming()
            }
            CallAction.CALL_CONNECTING.name -> {
                handleConnecting()
            }
            CallAction.CALL_CONNECTED.name -> {
                handleConnected()
            }
            CallAction.CALL_DISCONNECTED.name -> {
                handleDisconnected()
            }
            CallAction.CALL_BUSY.name -> {
                handleBusy()
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
                handleConnecting()
                CallService.startService(this@CallActivity, CallService.ACTION_CALL_ANSWER)
            } else {
                CallService.startService(this, CallService.ACTION_CALL_CANCEL)
                handleDisconnected()
            }
        }
    }

    private fun handleIncoming() {
        voice_tb.visibility = INVISIBLE
        mute_tb.visibility = INVISIBLE
        answer_fab.visibility = VISIBLE
        hangup_fab.visibility = INVISIBLE
        action_tv.text = "来电"
    }

    private fun handleOutgoing() {
        voice_tb.visibility = INVISIBLE
        mute_tb.visibility = INVISIBLE
        answer_fab.visibility = INVISIBLE
        hangup_fab.visibility = VISIBLE
        action_tv.text = "正在发起语音通话..."
    }

    private fun handleConnecting() {
        voice_tb.visibility = VISIBLE
        mute_tb.visibility = VISIBLE
        answer_fab.visibility = INVISIBLE
        hangup_fab.visibility = VISIBLE
        action_tv.text = "正在连接..."
    }

    private fun handleRinging() {
        voice_tb.visibility = INVISIBLE
        mute_tb.visibility = INVISIBLE
        answer_fab.visibility = INVISIBLE
        hangup_fab.visibility = VISIBLE
        action_tv.text = "等待对方接听..."
    }

    private fun handleConnected() {
        voice_tb.visibility = VISIBLE
        mute_tb.visibility = VISIBLE
        answer_fab.visibility = INVISIBLE
        hangup_fab.visibility = VISIBLE
        action_tv.text = 0L.formatMillis()
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

    private val timeRunnable: Runnable by lazy {
        Runnable {
            connectedTime++
            action_tv.text = (connectedTime * 1000L).formatMillis()
            action_tv.postDelayed(timeRunnable, 1000)
        }
    }

    enum class CallAction {
        // Normal states
        CALL_INCOMING,
        CALL_OUTGOING,
        CALL_CONNECTING,
        CALL_CONNECTED,
        CALL_BUSY,
        CALL_DISCONNECTED,

        // Error states
        NETWORK_FAILURE,
        RECIPIENT_UNAVAILABLE,
        NO_SUCH_USER,
        UNTRUSTED_IDENTITY,
    }

    companion object {
        const val TAG = "CallActivity"

        const val ARGS_ANSWER = "answer"
        const val ARGS_VIDEO = "video"

        fun show(context: Context, answer: User? = null, action: String? = null, video: Boolean = false) {
            Intent(context, CallActivity::class.java).apply {
                this.action = action
                putExtra(ARGS_ANSWER, answer)
                putExtra(ARGS_VIDEO, video)
            }.run {
                context.startActivity(this)
            }
        }

        fun disconnected(context: Context) {
            show(context, action = CallAction.CALL_DISCONNECTED.name)
        }
    }
}
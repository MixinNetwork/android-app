package one.mixin.android.ui.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.getSystemService
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.view_call_button.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.belowOreo
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.fastBlur
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.User
import one.mixin.android.webrtc.CallService
import one.mixin.android.widget.CallButton
import timber.log.Timber

class CallActivity : BaseActivity(), SensorEventListener {

    @Inject
    lateinit var callState: CallStateLiveData

    private var sensorManager: SensorManager? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Call
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Call
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_call)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        sensorManager = getSystemService()
        powerManager = getSystemService()
        wakeLock = powerManager?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "mixin")
        val answer = intent.getParcelableExtra<User?>(ARGS_ANSWER)
        if (answer != null) {
            name_tv.text = answer.fullName
            avatar.setInfo(answer.fullName, answer.avatarUrl, answer.userId)
            avatar.setTextSize(48f)
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
                CallService.muteAudio(this@CallActivity, checked)
            }
        })
        voice_cb.setOnCheckedChangeListener(object : CallButton.OnCheckedChangeListener {
            override fun onCheckedChanged(id: Int, checked: Boolean) {
                CallService.speakerPhone(this@CallActivity, checked)
            }
        })

        callState.observe(this, Observer { callInfo ->
            when (callInfo.callState) {
                CallService.CallState.STATE_DIALING -> {
                    volumeControlStream = AudioManager.STREAM_VOICE_CALL
                    call_cl.post { handleDialingConnecting() }
                }
                CallService.CallState.STATE_RINGING -> {
                    call_cl.post { handleRinging() }
                }
                CallService.CallState.STATE_ANSWERING -> {
                    call_cl.post { handleAnswering() }
                }
                CallService.CallState.STATE_CONNECTED -> {
                    call_cl.post { handleConnected() }
                }
                CallService.CallState.STATE_BUSY -> {
                    call_cl.post { handleBusy() }
                }
                CallService.CallState.STATE_IDLE -> {
                    call_cl.post { handleDisconnected() }
                }
            }
        })

        volumeControlStream = AudioManager.STREAM_VOICE_CALL

        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        window.statusBarColor = Color.TRANSPARENT
    }

    override fun onResume() {
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(TYPE_PROXIMITY),
            SensorManager.SENSOR_DELAY_UI
        )
        if (callState.connectedTime != null) {
            startTimer()
        }
        super.onResume()
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopTimber()
        super.onPause()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (event.sensor.type == TYPE_PROXIMITY) {
            if (values[0] == 0.0f) {
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(10 * 60 * 1000L)
                }
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (callState.isIdle()) {
            handleHangup()
        }
        handleDisconnected()
    }

    private fun handleHangup() {
        callState.handleHangup(this)
    }

    private fun setBlurBg(url: String) = lifecycleScope.launch(Dispatchers.IO) {
        if (url.isBlank()) return@launch
        try {
            val bitmap = Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .submit()
                .get(10, TimeUnit.SECONDS)
                .fastBlur(1f, 10)
            withContext(Dispatchers.Main) {
                bitmap?.let { bitmap ->
                    blur_iv.setImageBitmap(bitmap)
                }
            }
        } catch (e: TimeoutException) {
            Timber.e(e)
        } catch (e: GlideException) {
            Timber.e(e)
        }
    }

    private fun handleAnswer() {
        RxPermissions(this)
            .request(Manifest.permission.RECORD_AUDIO)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    handleAnswering()
                    CallService.answer(this@CallActivity)
                } else {
                    callState.handleHangup(this@CallActivity)
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
        voice_cb.visibility = INVISIBLE
        mute_cb.visibility = INVISIBLE
        answer_cb.fadeOut()
        moveHangup(true, 250)
        action_tv.text = getString(R.string.call_connecting)
    }

    private fun handleConnected() {
        voice_cb.fadeIn()
        mute_cb.fadeIn()
        answer_cb.fadeOut()
        moveHangup(true, 250)
        startTimer()
    }

    private fun handleDisconnected() {
        finishAndRemoveTask()
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

    private var timer: Timer? = null

    private fun startTimer() {
        timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (callState.connectedTime != null) {
                        val duration = System.currentTimeMillis() - callState.connectedTime!!
                        action_tv.text = duration.formatMillis()
                    }
                }
            }
        }
        timer?.schedule(timerTask, 0, 1000)
    }

    private fun stopTimber() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    companion object {
        const val TAG = "CallActivity"

        const val ARGS_ANSWER = "answer"

        fun show(context: Context, answer: User? = null) {
            Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ARGS_ANSWER, answer)
            }.run {
                context.startActivity(this)
            }
        }
    }
}

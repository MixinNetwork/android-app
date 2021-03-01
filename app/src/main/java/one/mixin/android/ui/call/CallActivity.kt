package one.mixin.android.ui.call

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.ActivityCallBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.belowOreo
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.fastBlur
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.isLandscape
import one.mixin.android.extension.isNotchScreen
import one.mixin.android.extension.realSize
import one.mixin.android.extension.showPipPermissionNotification
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService
import one.mixin.android.webrtc.acceptInvite
import one.mixin.android.webrtc.answerCall
import one.mixin.android.webrtc.logCallState
import one.mixin.android.webrtc.muteAudio
import one.mixin.android.webrtc.speakerPhone
import one.mixin.android.widget.CallButton
import one.mixin.android.widget.PipCallView
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : BaseActivity(), SensorEventListener {

    private val viewModel by viewModels<CallViewModel>()
    private val binding by viewBinding(ActivityCallBinding::inflate)

    @Inject
    lateinit var callState: CallStateLiveData

    private var sensorManager: SensorManager? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var userAdapter: CallUserAdapter? = null

    private var permissionAlert: AlertDialog? = null
    private var setClicked = false

    private val self = Session.getAccount()!!.toUser()

    private val pipCallView by lazy {
        PipCallView.get()
    }

    private var uiState: CallService.CallState = CallService.CallState.STATE_IDLE

    override fun getDefaultThemeId(): Int {
        return R.style.AppTheme_Call
    }

    override fun getNightThemeId(): Int {
        return R.style.AppTheme_Night_Call
    }

    private var join = false

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        belowOreo {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService<KeyguardManager>()
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        setContentView(binding.root)
        sensorManager = getSystemService()
        powerManager = getSystemService()
        wakeLock = powerManager?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "mixin")
        if (window?.isNotchScreen() == true) {
            binding.guidelineTop.updateLayoutParams<ConstraintLayout.LayoutParams> {
                guidePercent = 0.15f
            }
        }
        join = intent.getBooleanExtra(EXTRA_JOIN, false)
        if (callState.isGroupCall()) {
            binding.avatar.isVisible = false
            binding.usersRv.isVisible = true
            binding.addIv.isVisible = true
            if (userAdapter == null) {
                userAdapter = CallUserAdapter(self)
            }
            binding.usersRv.adapter = userAdapter
            userAdapter?.rvWidth = realSize().x * .8f
            callState.conversationId?.let {
                viewModel.observeConversationNameById(it).observe(
                    this,
                    { name ->
                        binding.nameTv?.text = name
                    }
                )
            }
            refreshUsers()
        } else {
            binding.avatar.isVisible = true
            binding.usersRv.isVisible = false
            binding.addIv.isVisible = false
            val callee = callState.user
            if (callee != null) {
                binding.nameTv.text = callee.fullName
                binding.avatar.setInfo(callee.fullName, callee.avatarUrl, callee.userId)
                binding.avatar.setTextSize(48f)
                if (callee.avatarUrl != null) {
                    setBlurBg(callee.avatarUrl)
                }
            }
        }
        binding.pipIv.setOnClickListener {
            switch2Pip()
        }
        binding.addIv.setOnClickListener {
            if (callState.isGroupCall() && callState.conversationId != null) {
                GroupUsersBottomSheetDialogFragment.newInstance(callState.conversationId!!)
                    .showNow(supportFragmentManager, GroupUsersBottomSheetDialogFragment.TAG)
            }
        }
        binding.hangupCb.setOnClickListener {
            hangup()
        }
        binding.answerCb.setOnClickListener {
            handleAnswer()
        }
        binding.closeIv.setOnClickListener {
            hangup()
        }
        binding.encryptionTv.setOnClickListener {
            showE2EETip()
        }
        binding.muteCb.setOnCheckedChangeListener(
            object : CallButton.OnCheckedChangeListener {
                override fun onCheckedChanged(id: Int, checked: Boolean) {
                    if (callState.isGroupCall()) {
                        muteAudio<GroupCallService>(this@CallActivity, checked)
                    } else if (callState.isVoiceCall()) {
                        muteAudio<VoiceCallService>(this@CallActivity, checked)
                    }
                }
            }
        )
        binding.voiceCb.setOnCheckedChangeListener(
            object : CallButton.OnCheckedChangeListener {
                override fun onCheckedChanged(id: Int, checked: Boolean) {
                    if (callState.isGroupCall()) {
                        speakerPhone<GroupCallService>(this@CallActivity, checked)
                    } else if (callState.isVoiceCall()) {
                        speakerPhone<VoiceCallService>(this@CallActivity, checked)
                    }
                }
            }
        )
        binding.voiceCb.setOnLongClickListener {
            if (callState.isGroupCall()) {
                logCallState<GroupCallService>(this@CallActivity)
            } else {
                logCallState<VoiceCallService>(this@CallActivity)
            }
            return@setOnLongClickListener true
        }
        updateUI()

        callState.observe(
            this,
            Observer { state ->
                // if plan to join a group voice, do not show self before answering
                if (join && state >= CallService.CallState.STATE_ANSWERING) {
                    join = false
                }

                updateUI()
                if (callState.isGroupCall()) {
                    refreshUsers()
                }
                if (state == CallService.CallState.STATE_IDLE) {
                    binding.callCl.post { handleDisconnected() }
                    return@Observer
                }
                if (uiState >= state) {
                    if (
                        uiState == CallService.CallState.STATE_CONNECTED && state == CallService.CallState.STATE_CONNECTED
                    ) {
                        handleConnected(callState.disconnected)
                    }
                    return@Observer
                }

                uiState = state

                when (state) {
                    CallService.CallState.STATE_DIALING -> {
                        binding.callCl.post { handleDialing() }
                    }
                    CallService.CallState.STATE_RINGING -> {
                        binding.callCl.post {
                            if (join) {
                                handleJoin()
                            } else {
                                handleRinging()
                            }
                        }
                    }
                    CallService.CallState.STATE_ANSWERING -> {
                        binding.callCl.post { handleAnswering() }
                    }
                    CallService.CallState.STATE_CONNECTED -> {
                        binding.callCl.post { handleConnected(callState.disconnected) }
                    }
                    CallService.CallState.STATE_BUSY -> {
                        binding.callCl.post { handleBusy() }
                    }
                }
            }
        )

        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        window.statusBarColor = Color.TRANSPARENT

        if (pipCallView.shown) {
            pipCallView.close()
        }
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
        stopTimer()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (powerManager?.isInteractive == false) {
            return
        }
        if (callState.isNotIdle()) {
            if (!checkPipPermission()) {
                if (!setClicked) {
                    showPipPermissionNotification(CallActivity::class.java, getString(R.string.call_pip_permission))
                }
                return
            }
            switch2Pip()
        }
    }

    override fun onBackPressed() {
        if (callState.isBeforeAnswering()) {
            return
        }
        super.onBackPressed()
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

    private fun hangup() {
        callState.handleHangup(this, join)
        finishAndRemoveTask()
    }

    private fun updateUI() {
        binding.muteCb.isChecked = !callState.audioEnable
        binding.voiceCb.isChecked = callState.speakerEnable
        binding.voiceCb.isEnabled = !callState.customAudioDeviceAvailable
        binding.pipIv.isVisible = callState.isConnected()
        if (binding.pipIv.isVisible) {
            binding.closeIv.isVisible = false
        }
        binding.addIv.isVisible = callState.isConnected() && callState.isGroupCall()
    }

    private fun refreshUsers() = lifecycleScope.launch {
        val cid = callState.conversationId ?: return@launch
        val us = callState.getUsers(cid)
        val callees = mutableListOf<String>().apply { us?.let { addAll(it) } }
        var layoutManager: GridLayoutManager? = binding.usersRv.layoutManager as GridLayoutManager?
        if (layoutManager == null) {
            layoutManager = GridLayoutManager(this@CallActivity, 3)
            binding.usersRv.layoutManager = layoutManager
        }

        if (callees.isNullOrEmpty()) {
            userAdapter?.submitList(null)
        } else {
            val last = callees.lastOrNull()
            if (callees.size == 1 && last == self.userId) {
                userAdapter?.submitList(listOf(self)) {
                    layoutManager.spanCount = getSpanCount(1)
                }
                return@launch
            }
            if (last != self.userId) {
                val exists = callees.remove(self.userId)
                if (!join || exists) {
                    callees.add(self.userId)
                }
            }
            val users = viewModel.findMultiUsersByIds(callees.toSet())
            val orderByIds = callees.withIndex().associate { it.value to it.index }
            val sortedUsers = users.sortedBy { orderByIds[it.userId] }
            userAdapter?.submitList(sortedUsers) {
                layoutManager.spanCount = getSpanCount(sortedUsers.size)
            }
        }
        val currentGuestsNotConnected = userAdapter?.guestsNotConnected
        val newGuestsNotConnected = callState.getPendingUsers(cid)
        if (currentGuestsNotConnected != newGuestsNotConnected) {
            userAdapter?.guestsNotConnected = newGuestsNotConnected
            userAdapter?.notifyDataSetChanged()
        }
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
                    binding.blurIv.setImageBitmap(bitmap)
                }
            }
        } catch (e: TimeoutException) {
            Timber.e(e)
        } catch (e: GlideException) {
            Timber.e(e)
        }
    }

    private fun showE2EETip() {
        alertDialogBuilder()
            .setMessage(R.string.end_to_end_encryption_tip)
            .setNeutralButton(R.string.chat_learn) { dialog, _ ->
                WebActivity.show(
                    this,
                    getString(R.string.chat_waiting_url),
                    callState.conversationId
                )
                dialog.dismiss()
            }
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleAnswer() {
        RxPermissions(this)
            .request(Manifest.permission.RECORD_AUDIO)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    handleAnswering()
                    if (callState.isGroupCall()) {
                        acceptInvite(this@CallActivity)
                    } else if (callState.isVoiceCall()) {
                        answerCall(this@CallActivity)
                    }
                } else {
                    callState.handleHangup(this@CallActivity)
                    handleDisconnected()
                }
            }
    }

    private var pipAnimationInProgress = false
    private fun switch2Pip() {
        if (!checkPipPermission() || pipAnimationInProgress) {
            return
        }
        pipAnimationInProgress = true
        val rect = PipCallView.getPipRect()
        val windowView = binding.callCl
        val isLandscape = isLandscape()
        if (isLandscape) {
            val screenHeight = realSize().y
            if (rect.width > screenHeight) {
                val ratio = rect.width / rect.height
                rect.height = screenHeight / ratio
                rect.width = screenHeight.toFloat()
            }
        }
        val width = if (isLandscape) windowView.height else windowView.width
        val scale = (if (isLandscape) rect.height else rect.width) / width
        window.decorView.systemUiVisibility =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        pipCallView.show(this, callState.connectedTime, callState)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(windowView, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(windowView, View.SCALE_Y, scale),
                ObjectAnimator.ofFloat(
                    windowView,
                    View.TRANSLATION_X,
                    rect.x - realSize().x * (1f - scale) / 2
                ),
                ObjectAnimator.ofFloat(
                    windowView,
                    View.TRANSLATION_Y,
                    rect.y - statusBarHeight() - (windowView.height - rect.height) / 2
                )
            )
            interpolator = DecelerateInterpolator()
            duration = 250
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        if (!SystemUIManager.hasCutOut(window)) {
                            SystemUIManager.clearStyle(window)
                        }
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        pipAnimationInProgress = false

                        overridePendingTransition(0, 0)
                        finish()
                    }
                }
            )
            start()
        }
    }

    private fun checkPipPermission() =
        checkInlinePermissions {
            if (setClicked) {
                setClicked = false
                return@checkInlinePermissions
            }
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions

            permissionAlert = AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.call_pip_permission)
                .setPositiveButton(R.string.live_setting) { dialog, _ ->
                    try {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    dialog.dismiss()
                    setClicked = true
                }.show()
        }

    private fun handleDialing() {
        binding.voiceCb.isVisible = true
        binding.muteCb.isVisible = true
        binding.answerCb.isVisible = false
        moveHangup(true, 0)
        binding.actionTv.text = getString(R.string.call_notification_outgoing)
    }

    private fun handleRinging() {
        binding.voiceCb.isVisible = false
        binding.muteCb.isVisible = false
        binding.answerCb.isVisible = true
        moveHangup(false, 0)
        binding.actionTv.text = getString(R.string.call_notification_incoming_voice)
    }

    private fun handleJoin() {
        binding.voiceCb.isVisible = false
        binding.muteCb.isVisible = false
        binding.answerCb.isVisible = true
        binding.hangupCb.isVisible = false
        binding.answerCb.updateLayoutParams<ConstraintLayout.LayoutParams> {
            horizontalBias = 0.5f
        }
        binding.closeIv.isVisible = true
    }

    private fun handleAnswering() {
        binding.voiceCb.fadeIn()
        binding.muteCb.fadeIn()
        binding.answerCb.fadeOut()
        moveHangup(true, 250)
        binding.actionTv.text = getString(R.string.call_connecting)
    }

    private fun handleConnected(disconnected: Boolean) {
        if (!binding.voiceCb.isVisible) {
            binding.voiceCb.fadeIn()
        }
        if (!binding.muteCb.isVisible) {
            binding.muteCb.fadeIn()
        }
        if (binding.answerCb.isVisible) {
            binding.answerCb.fadeOut()
        }
        moveHangup(true, 250)
        if (disconnected) {
            binding.tipTv.isVisible = true
        } else {
            binding.tipTv.isVisible = false
            startTimer()
        }
    }

    private fun handleDisconnected() {
        finishAndRemoveTask()
    }

    private fun handleBusy() {
        handleDisconnected()
    }

    private fun moveHangup(center: Boolean, duration: Long) {
        binding.hangupCb.visibility = VISIBLE
        val constraintSet = ConstraintSet().apply {
            clone(binding.callCl)
            setHorizontalBias(binding.hangupCb.id, if (center) 0.5f else 0.1f)
        }
        val transition = AutoTransition().apply {
            this.duration = duration
        }
        TransitionManager.beginDelayedTransition(binding.callCl, transition)
        constraintSet.applyTo(binding.callCl)
    }

    private var timer: Timer? = null

    private fun startTimer() {
        timer = Timer(true)
        val timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (callState.connectedTime != null) {
                        val duration = System.currentTimeMillis() - callState.connectedTime!!
                        binding.actionTv.text = duration.formatMillis()
                    }
                }
            }
        }
        timer?.schedule(timerTask, 0, 1000)
    }

    private fun stopTimer() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    private fun getSpanCount(size: Int) = when {
        size <= 1 -> 1
        size == 2 -> 2
        size <= 9 -> 3
        else -> 4
    }

    companion object {
        const val TAG = "CallActivity"
        const val EXTRA_JOIN = "extra_join"
        const val CHANNEL_PIP_PERMISSION = "channel_pip_permission"
        const val ID_PIP_PERMISSION = 313389

        fun show(context: Context, join: Boolean = false) {
            Intent(context, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_JOIN, join)
            }.run {
                context.startActivity(this)
            }
        }
    }
}

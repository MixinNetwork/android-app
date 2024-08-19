package one.mixin.android.ui.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.databinding.FragmentCallBottomSheetBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.checkInlinePermissions
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.showPipPermissionNotification
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.reportException
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.CallUser
import one.mixin.android.vo.ParticipantRole
import one.mixin.android.webrtc.CallDebugLiveData
import one.mixin.android.webrtc.CallService
import one.mixin.android.webrtc.GroupCallService
import one.mixin.android.webrtc.VoiceCallService
import one.mixin.android.webrtc.acceptInvite
import one.mixin.android.webrtc.answerCall
import one.mixin.android.webrtc.logCallState
import one.mixin.android.webrtc.muteAudio
import one.mixin.android.webrtc.next
import one.mixin.android.webrtc.speakerPhone
import one.mixin.android.widget.CallButton
import one.mixin.android.widget.MixinBottomSheetDialog
import one.mixin.android.widget.PipCallView
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class CallBottomSheetDialogFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "CallBottomSheetDialogFragment"
        const val EXTRA_JOIN = "extra_join"

        @SuppressLint("StaticFieldLeak")
        private var instant: CallBottomSheetDialogFragment? = null

        fun newInstance(
            join: Boolean,
        ): CallBottomSheetDialogFragment {
            try {
                instant?.dismiss()
            } catch (ignored: IllegalStateException) {
            }
            instant = null
            return CallBottomSheetDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putBoolean(CallActivity.EXTRA_JOIN, join)
                    }
                instant = this
            }
        }
    }

    private val stopScope = scope(Lifecycle.Event.ON_STOP)
    private lateinit var contentView: View

    @Inject
    lateinit var callState: CallStateLiveData

    @Inject
    lateinit var callDebugState: CallDebugLiveData
    lateinit var self: CallUser
    private var uiState: CallService.CallState = CallService.CallState.STATE_IDLE
    private val viewModel by viewModels<CallViewModel>()
    private var join = false

    private var _binding: FragmentCallBottomSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun getTheme() = R.style.MixinBottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme).apply {
            dismissWithAnimation = true
        }
    }

    private val peekHeight by lazy {
        480.dp
    }

    private var translationOffset by Delegates.notNull<Float>()

    private var groupName: String? = null

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        _binding = FragmentCallBottomSheetBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as BottomSheetBehavior<*>
        behavior.peekHeight = peekHeight
        binding.root.doOnPreDraw { root ->
            translationOffset = (peekHeight - root.measuredHeight).toFloat()
            binding.participants.translationY = translationOffset
            binding.bottomLayout.translationY = translationOffset
        }
        (binding.avatarLl.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 132.dp
        behavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(
                    bottomSheet: View,
                    newState: Int,
                ) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        safeDismiss()
                    }
                }

                override fun onSlide(
                    bottomSheet: View,
                    slideOffset: Float,
                ) {
                    userAdapter?.let {
                        if (it.itemCount > 8) {
                            binding.participants.translationY = 0f
                        } else {
                            binding.participants.translationY = translationOffset * (1 - slideOffset)
                        }
                    }
                    binding.bottomLayout.translationY = translationOffset * (1 - slideOffset)
                }
            },
        )
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
        join = requireArguments().getBoolean(EXTRA_JOIN, false)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val cid = callState.conversationId
                val account = Session.getAccount()!!
                self =
                    if (cid == null) {
                        CallUser(
                            account.userId,
                            account.identityNumber,
                            account.fullName,
                            account.avatarUrl,
                            "",
                            false,
                            null,
                            account.membership
                        )
                    } else {
                        var callUser = viewModel.findSelfCallUser(cid, account.userId)
                        if (callUser == null) {
                            callUser =
                                CallUser(
                                    account.userId,
                                    account.identityNumber,
                                    account.fullName,
                                    account.avatarUrl,
                                    "",
                                    false,
                                    null,
                                    account.membership
                                )
                            viewModel.refreshConversation(cid)
                        }
                        callUser
                    }
                if (callState.isGroupCall()) {
                    withContext(Dispatchers.IO) {
                        groupName = viewModel.getConversationNameById(requireNotNull(cid))
                    }
                    binding.title.text = "$groupName"
                    binding.avatarLl.isVisible = false
                    binding.usersRv.isVisible = true
                    binding.participants.isVisible = true
                    setAdapter()
                    refreshUsers()
                } else {
                    binding.title.text = getString(R.string.Call)
                    binding.avatarLl.isVisible = true
                    binding.usersRv.isVisible = false
                    val callee = callState.user
                    if (callee != null) {
                        binding.nameTv.setName(callee)
                        binding.avatar.setInfo(callee.fullName, callee.avatarUrl, callee.userId)
                        binding.avatar.setTextSize(48f)
                        binding.avatar.clicks()
                            .observeOn(AndroidSchedulers.mainThread())
                            .throttleFirst(500, TimeUnit.MILLISECONDS)
                            .autoDispose(stopScope)
                            .subscribe {
                                showUserBottom(parentFragmentManager, callee)
                            }
                        binding.nameTv.clicks()
                            .observeOn(AndroidSchedulers.mainThread())
                            .throttleFirst(500, TimeUnit.MILLISECONDS)
                            .autoDispose(stopScope)
                            .subscribe {
                                showUserBottom(parentFragmentManager, callee)
                            }
                    }
                }

                binding.hangupCb.setOnClickListener {
                    hangup()
                }
                binding.answerCb.setOnClickListener {
                    checkBlueToothConnectPermissions { handleAnswer() }
                }
                binding.closeIb.setOnClickListener {
                    hangup()
                }
                binding.minimizeIb.setOnClickListener {
                    if (!PipCallView.get().shown) {
                        if (!checkPipPermission(requireActivity())) {
                            return@setOnClickListener
                        }
                        PipCallView.get().show(callState.connectedTime, callState)
                    }
                    dismiss()
                }
                binding.declineTv.setOnClickListener {
                    hangup()
                }
                if (BuildConfig.DEBUG) {
                    binding.title.setOnClickListener {
                        var curState = callDebugState.debugState
                        curState = curState.next()
                        callDebugState.debugState = curState
                        toast("CallDebugState $curState")
                    }
                }
                binding.subTitle.setOnClickListener {
                    showE2EETip()
                }
                binding.muteCb.setOnCheckedChangeListener(
                    object : CallButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            id: Int,
                            checked: Boolean,
                        ) {
                            if (callState.isGroupCall()) {
                                muteAudio<GroupCallService>(requireContext(), checked)
                            } else if (callState.isVoiceCall()) {
                                muteAudio<VoiceCallService>(requireContext(), checked)
                            }
                        }
                    },
                )
                binding.voiceCb.setOnCheckedChangeListener(
                    object : CallButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(
                            id: Int,
                            checked: Boolean,
                        ) {
                            if (callState.isGroupCall()) {
                                speakerPhone<GroupCallService>(requireContext(), checked)
                            } else if (callState.isVoiceCall()) {
                                speakerPhone<VoiceCallService>(requireContext(), checked)
                            }
                        }
                    },
                )
                binding.title.setOnLongClickListener {
                    if (callState.isGroupCall()) {
                        logCallState<GroupCallService>(requireContext())
                    } else {
                        logCallState<VoiceCallService>(requireContext())
                    }
                    return@setOnLongClickListener true
                }
                updateUI()
                callState.observe(
                    this@CallBottomSheetDialogFragment,
                    Observer { state ->
                        // if plan to join a group voice, do not show self before answering
                        if (join && state >= CallService.CallState.STATE_ANSWERING) {
                            join = false
                        }

                        updateUI()
                        if (callState.isGroupCall()) {
                            refreshUsers()

                            if ((
                                    state == CallService.CallState.STATE_IDLE ||
                                        state == CallService.CallState.STATE_RINGING
                                ) &&
                                callState.needMuteWhenJoin(requireNotNull(cid))
                            ) {
                                updateTitle(getString(R.string.chat_group_call_mute))
                            }
                        }
                        if (state == CallService.CallState.STATE_IDLE) {
                            if (callState.isNoneCallType()) {
                                handleDisconnected()
                            } else {
                                cid?.let {
                                    val groupCallState = callState.getGroupCallStateOrNull(cid)
                                    if (groupCallState == null || groupCallState.users?.isEmpty() == true) {
                                        toast(R.string.chat_group_call_end)
                                        dismiss()
                                    }
                                }
                            }
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
                                contentView.post { handleDialing() }
                            }

                            CallService.CallState.STATE_RINGING -> {
                                contentView.post {
                                    if (join) {
                                        handleJoin()
                                    } else {
                                        handleRinging()
                                    }
                                }
                            }

                            CallService.CallState.STATE_ANSWERING -> {
                                contentView.post { handleAnswering() }
                            }

                            CallService.CallState.STATE_CONNECTED -> {
                                contentView.post { handleConnected(callState.disconnected) }
                            }

                            CallService.CallState.STATE_BUSY -> {
                                contentView.post { handleBusy() }
                            }

                            else -> {
                                // Do nothing
                            }
                        }
                    },
                )
                if (callState.state == CallService.CallState.STATE_RINGING) {
                    binding.closeIb.isVisible = true
                    binding.minimizeIb.isVisible = false
                }
            }
        }
        dialog.setOnKeyListener { _, keyCode, _ ->
            keyCode == KeyEvent.KEYCODE_BACK && callState.isBeforeAnswering()
        }
    }

    private fun setAdapter() {
        if (userAdapter == null) {
            userAdapter =
                CallUserAdapter(self) { userId ->
                    if (userId != null) {
                        lifecycleScope.launch {
                            val user = viewModel.suspendFindUserById(userId) ?: return@launch
                            showUserBottom(parentFragmentManager, user)
                        }
                    } else if (callState.isGroupCall() && callState.conversationId != null) {
                        GroupUsersBottomSheetDialogFragment.newInstance(callState.conversationId!!)
                            .showNow(
                                parentFragmentManager,
                                GroupUsersBottomSheetDialogFragment.TAG,
                            )
                    }
                }
        }
        binding.usersRv.adapter = userAdapter
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    private fun startTimer() {
        timer = Timer(true)
        timerTask?.cancel()
        timerTask =
            object : TimerTask() {
                override fun run() {
                    lifecycleScope.launch {
                        if (callState.connectedTime != null) {
                            val duration = System.currentTimeMillis() - callState.connectedTime!!
                            updateTitle(duration.formatMillis())
                        }
                    }
                }
            }
        timer?.schedule(timerTask, 0, 1000)
    }

    private fun stopTimer() {
        timerTask?.cancel()
        timerTask = null
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    private fun updateUI() {
        binding.muteCb.isChecked = !callState.audioEnable
        binding.voiceCb.isChecked = callState.speakerEnable
        binding.voiceCb.isEnabled = !callState.customAudioDeviceAvailable
    }

    private fun hangup() {
        callState.handleHangup(requireContext(), join)
    }

    private fun handleAnswer() {
        RxPermissions(this)
            .request(Manifest.permission.RECORD_AUDIO)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    handleAnswering()
                    if (callState.isGroupCall()) {
                        acceptInvite(requireContext())
                    } else if (callState.isVoiceCall()) {
                        answerCall(requireContext())
                    }
                } else {
                    callState.handleHangup(requireContext())
                    handleDisconnected()
                }
            }
    }

    private fun handleDisconnected() {
        dismiss()
    }

    private fun handleBusy() {
        handleDisconnected()
    }

    override fun dismiss() {
        safeDismiss()
    }

    private fun handleRinging() {
        binding.voiceCb.isVisible = false
        binding.muteCb.isVisible = false
        binding.answerCb.isVisible = true
        binding.hangupCb.isVisible = false
        binding.closeIb.isVisible = true
        binding.minimizeIb.isVisible = false
        binding.declineTv.isVisible = true
        updateTitle(getString(R.string.Incoming_voice_call))
    }

    private fun handleJoin() {
        binding.voiceCb.isVisible = false
        binding.muteCb.isVisible = false
        binding.answerCb.isVisible = true
        binding.hangupCb.isVisible = false
        binding.closeIb.isVisible = true
        binding.minimizeIb.isVisible = false
        binding.declineTv.isVisible = false
    }

    private fun handleDialing() {
        binding.voiceCb.isVisible = true
        binding.muteCb.isVisible = true
        binding.answerCb.isVisible = false
        binding.hangupCb.isVisible = true
        binding.closeIb.isVisible = false
        binding.minimizeIb.isVisible = true
        binding.declineTv.isVisible = false
        updateTitle(getString(R.string.Calling))
    }

    private fun handleAnswering() {
        binding.voiceCb.fadeIn()
        binding.muteCb.fadeIn()
        binding.answerCb.fadeOut()
        binding.hangupCb.fadeIn()
        binding.closeIb.isVisible = false
        binding.minimizeIb.isVisible = true
        binding.declineTv.isVisible = false
        updateTitle(getString(R.string.in_connecting))
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
            binding.hangupCb.fadeIn()
        }
        if (disconnected) {
            stopTimer()
            updateTitle(getString(R.string.Connection_unstable))
        } else {
            startTimer()
        }
        binding.closeIb.isVisible = false
        binding.minimizeIb.isVisible = true
        binding.declineTv.isVisible = false
    }

    private fun updateTitle(content: String) {
        binding.title.text =
            if (callState.isGroupCall()) {
                "$groupName"
            } else {
                getString(
                    R.string.Call,
                )
            }
        binding.callStatus.text = content
    }

    private var userAdapter: CallUserAdapter? = null

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshUsers() =
        lifecycleScope.launch {
            val cid = callState.conversationId ?: return@launch
            val us = callState.getUsers(cid)
            val calls = mutableListOf<String>().apply { us?.let { addAll(it) } }
            if (binding.usersRv.layoutManager == null) {
                binding.usersRv.layoutManager = GridLayoutManager(requireContext(), 4)
            }
            if (calls.isEmpty()) {
                userAdapter?.submitList(null)
                binding.participants.text = requireContext().resources.getQuantityString(R.plurals.title_participants, 0, 0)
            } else {
                val last = calls.lastOrNull()
                if (calls.size == 1 && last == self.userId) {
                    userAdapter?.submitList(listOf(self))
                    binding.participants.text = requireContext().resources.getQuantityString(R.plurals.title_participants, 1, 1)
                    binding.participants.translationY = binding.bottomLayout.translationY
                    return@launch
                }
                val users =
                    viewModel.findMultiCallUsersByIds(cid, calls.toSet())
                        .sortedWith { u1, u2 ->
                            when {
                                u1.role == u2.role -> {
                                    return@sortedWith 0
                                }
                                u1.role == ParticipantRole.OWNER.name -> {
                                    return@sortedWith -1
                                }
                                u2.role == ParticipantRole.OWNER.name -> {
                                    return@sortedWith 1
                                }
                                u1.role == ParticipantRole.ADMIN.name -> {
                                    return@sortedWith -1
                                }
                                else -> {
                                    return@sortedWith 1
                                }
                            }
                        }
                userAdapter?.apply {
                    submitList(users)
                    if (itemCount > 8) {
                        binding.participants.translationY = 0f
                    } else {
                        binding.participants.translationY = binding.bottomLayout.translationY
                    }
                }
                binding.participants.text =
                    requireContext().resources.getQuantityString(
                        R.plurals.title_participants,
                        users.size,
                        users.size,
                    )
            }
            val currentGuestsNotConnected = userAdapter?.guestsNotConnected
            val newGuestsNotConnected = callState.getPendingUsers(cid)
            if (currentGuestsNotConnected != newGuestsNotConnected) {
                userAdapter?.guestsNotConnected = newGuestsNotConnected
                userAdapter?.notifyDataSetChanged()
            }
        }

    private fun showE2EETip() {
        alertDialogBuilder()
            .setTitle(R.string.call_encryption_title)
            .setMessage(R.string.call_encryption_description)
            .setNeutralButton(R.string.Learn_More) { dialog, _ ->
                WebActivity.show(
                    requireContext(),
                    getString(R.string.secret_url),
                    callState.conversationId,
                )
                dialog.dismiss()
            }
            .setPositiveButton(R.string.OK) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
        if (PipCallView.get().shown) {
            PipCallView.get().close()
        }
    }

    override fun onResume() {
        if (callState.connectedTime != null) {
            startTimer()
        }
        super.onResume()
        if (binding.usersRv.adapter == null && ::self.isInitialized) {
            setAdapter()
        }
    }

    override fun onPause() {
        stopTimer()
        super.onPause()

        // this will make RecyclerView adapter call onViewDetachedFromWindow()
        // to prevent leak Fragment and Activity
        binding.usersRv.adapter = null
    }

    override fun onStop() {
        if (callState.isNotIdle()) {
            if (!requireActivity().checkInlinePermissions()) {
                if (!setClicked) {
                    requireActivity().showPipPermissionNotification(
                        CallActivity::class.java,
                        getString(R.string.call_pip_permission),
                    )
                }
            } else if (callState.isInUse() && checkPipPermission(requireContext())) {
                PipCallView.get().show(callState.connectedTime, callState)
            }
        }
        super.onStop()
    }

    override fun onDetach() {
        super.onDetach()
        instant = null
    }

    private fun safeDismiss() {
        if (isAdded) {
            dialog?.dismiss()
            dialog?.setOnDismissListener {
                try {
                    super.dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    Timber.w(e)
                } finally {
                    if (activity?.isFinishing == false) {
                        activity?.finish()
                    }
                }
            }
        } else {
            try {
                super.dismissAllowingStateLoss()
            } catch (e: IllegalStateException) {
                Timber.w(e)
            } finally {
                if (activity?.isFinishing == false) {
                    activity?.finish()
                }
            }
        }
    }

    fun onBackPressed(): Boolean {
        return callState.isBeforeAnswering()
    }

    private var permissionAlert: AlertDialog? = null
    private var setClicked = false

    private fun checkPipPermission(context: Context): Boolean =
        context.checkInlinePermissions {
            if (setClicked) {
                setClicked = false
                return@checkInlinePermissions
            }
            if (permissionAlert != null && permissionAlert!!.isShowing) return@checkInlinePermissions

            permissionAlert =
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.call_pip_permission)
                    .setPositiveButton(R.string.Settings) { dialog, _ ->
                        try {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${requireContext().packageName}"),
                                ),
                            )
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                        dialog.dismiss()
                        setClicked = true
                    }.show()
        }

    private fun checkBlueToothConnectPermissions(callback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RxPermissions(this)
                .request(Manifest.permission.BLUETOOTH_CONNECT)
                .autoDispose(stopScope).subscribe({ granted ->
                    if (granted) {
                        callback.invoke()
                    } else {
                        context?.openPermissionSetting()
                    }
                }, {
                    reportException(it)
                })
        } else {
            callback.invoke()
        }
    }
}

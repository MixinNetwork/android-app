@file:Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")

package one.mixin.android.ui.transfer

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import one.mixin.android.Constants
import one.mixin.android.Constants.Scheme.DEVICE_TRANSFER
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.event.SpeedEvent
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.isConnectedToWiFi
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.job.BaseJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendPlaintextJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.transfer.status.TransferStatus
import one.mixin.android.ui.transfer.status.TransferStatusLiveData
import one.mixin.android.ui.transfer.vo.CURRENT_TRANSFER_VERSION
import one.mixin.android.ui.transfer.vo.TransferCommand
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.generateConversationId
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalSerializationApi::class)
@AndroidEntryPoint
class TransferActivity : BaseActivity() {
    companion object {
        const val ARGS_STATUS = "args_status"

        private const val ARGS_TRANSFER_TO_PHONE = 1
        private const val ARGS_TRANSFER_TO_PC = 2
        private const val ARGS_RESTORE_FROM_PHONE = 3
        private const val ARGS_RESTORE_FROM_PC = 4

        fun showTransferToPhone(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_TRANSFER_TO_PHONE)
                },
            )
        }

        fun showTransferToPC(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_TRANSFER_TO_PC)
                },
            )
        }

        fun showRestoreFromPC(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_RESTORE_FROM_PC)
                },
            )
        }

        fun showRestoreFromPhone(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_RESTORE_FROM_PHONE)
                },
            )
        }
    }

    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    private val binding by viewBinding(ActivityTransferBinding::inflate)

    @Inject
    lateinit var transferServer: TransferServer

    @Inject
    lateinit var transferClient: TransferClient

    @Inject
    lateinit var status: TransferStatusLiveData

    @Inject
    lateinit var chatWebSocket: ChatWebSocket

    @Inject
    lateinit var transferSessionManager: TransferSessionManager

    private var activeSessionId: String? = null
    private var pairingDialog: Dialog? = null

    var shouldLogout = false

    override fun onBackPressed() {
        if (status.value in
            listOf(
                TransferStatus.INITIALIZING,
                TransferStatus.WAITING_MESSAGE,
                TransferStatus.WAITING_FOR_CONNECTION,
                TransferStatus.CREATED,
                TransferStatus.ERROR,
                TransferStatus.FINISHED,
            )
        ) {
            super.onBackPressed()
        } else {
            toast(R.string.cannot_exit_in_backup)
        }
    }

    private fun callbackScan(data: Intent?) {
        if (argsStatus != ARGS_RESTORE_FROM_PHONE) return
        val text = data?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT) ?: return
        val content =
            try {
                Uri.parse(text).getQueryParameter("data")
            } catch (e: Exception) {
                toast(R.string.Data_error)
                null
            } ?: return
        handleQrCodeContent(content)
    }

    private var dialog: Dialog? = null
    private var retryDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status.value = TransferStatus.INITIALIZING
        setContentView(binding.root)

        getScanResult =
            registerForActivityResult(
                CaptureActivity.CaptureContract(),
                activityResultRegistry,
                ::callbackScan,
            )
        binding.titleView.leftIb.setOnClickListener {
            onBackPressed()
        }
        initView()

        status.observe(this) { s ->
            when (s) {
                null, TransferStatus.INITIALIZING -> {
                    initView()
                }

                TransferStatus.WAITING_MESSAGE,
                TransferStatus.CREATED, TransferStatus.WAITING_FOR_CONNECTION,
                -> {
                    if (argsStatus == ARGS_TRANSFER_TO_PHONE) {
                        binding.qrFl.isVisible = true
                        binding.initLl.isVisible = false
                        binding.waitingLl.isVisible = false
                    } else {
                        binding.qrFl.isVisible = false
                        binding.initLl.isVisible = true
                        binding.waitingLl.isVisible = false
                        binding.pbFl.isVisible = true
                        binding.pbTips.isVisible = true
                    }
                    binding.startTv.setText(R.string.Waiting)
                    binding.startTv.isEnabled = false
                    binding.start.isClickable = false
                    binding.selectLl.isVisible = false
                }

                TransferStatus.CONNECTING -> {
                    binding.qrFl.isVisible = false
                    binding.selectLl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.WAITING_FOR_VERIFICATION -> {
                    binding.qrFl.isVisible = false
                    binding.selectLl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.VERIFICATION_COMPLETED -> {
                    binding.qrFl.isVisible = false
                    binding.selectLl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.SYNCING -> {
                    binding.titleView.isInvisible = true
                    binding.selectLl.isVisible = false
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbLl.isVisible = true
                }

                TransferStatus.PROCESSING -> {
                    binding.titleView.isInvisible = true
                    binding.selectLl.isVisible = false
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbLl.isVisible = true
                    binding.progressTv.setText(R.string.transfer_process_title)
                    binding.progressDesc.setText(R.string.transfer_process_tip)
                }

                TransferStatus.ERROR -> {
                    binding.pbLl.isVisible = false
                    binding.selectLl.isVisible = false
                    binding.progressTv.setText(R.string.Transfer_error)
                    if (argsStatus == ARGS_TRANSFER_TO_PHONE) {
                        dialog?.dismiss()
                        if (retryDialog == null) {
                            retryDialog =
                                alertDialogBuilder()
                                    .setTitle(R.string.Transfer_error)
                                    .setCancelable(false)
                                    .setNegativeButton(R.string.Exit) { dialog, _ ->
                                        dialog.dismiss()
                                        finish()
                                        status.value = TransferStatus.INITIALIZING
                                    }
                                    .setPositiveButton(R.string.Retry) { dialog, _ ->
                                        dialog.dismiss()
                                        status.value = TransferStatus.INITIALIZING
                                        val accountId = Session.getAccountId() ?: return@setPositiveButton
                                        activeSessionId =
                                            transferSessionManager.initiateSession(
                                                direction = TransferDirection.TRANSFER_TO_PHONE,
                                                accountId = accountId,
                                            ).sessionId
                                        lifecycleScope.launch {
                                            transferServer.restartServer { transferCommandData ->
                                                lifecycleScope.launch(Dispatchers.Main) {
                                                    val qrCode =
                                                        gson.toJson(transferCommandData)
                                                            .base64Encode()
                                                            .run {
                                                                "$DEVICE_TRANSFER?data=$this"
                                                            }
                                                            .generateQRCode(240.dp).first
                                                    binding.qr.setImageBitmap(qrCode)
                                                    binding.qrFl.fadeIn()
                                                    binding.initLl.isVisible = false
                                                    binding.waitingLl.isVisible = false
                                                }
                                            }
                                        }
                                    }.create()
                        }
                        retryDialog?.show()
                    } else {
                        if (dialog == null) {
                            dialog =
                                alertDialogBuilder()
                                    .setTitle(R.string.Transfer_error)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.Confirm) { dialog, _ ->
                                        dialog.dismiss()
                                        finish()
                                        status.value = TransferStatus.INITIALIZING
                                    }.create()
                            dialog?.show()
                        }
                    }
                }

                TransferStatus.FINISHED -> {
                    binding.pbLl.isVisible = false
                    binding.progressTv.setText(R.string.Transfer_completed)
                    if (dialog == null) {
                        dialog =
                            alertDialogBuilder()
                                .setTitle(R.string.Transfer_completed)
                                .setCancelable(false)
                                .setPositiveButton(R.string.Confirm) { dialog, _ ->
                                    dialog.dismiss()
                                    if (argsStatus == ARGS_RESTORE_FROM_PHONE) {
                                        InitializeActivity.showLoading(this, clear = true)
                                        defaultSharedPreferences.putBoolean(
                                            Constants.Account.PREF_RESTORE,
                                            false,
                                        )
                                    }
                                    status.value = TransferStatus.INITIALIZING
                                    finish()
                                }.create()
                        dialog?.show()
                    }
                }
            }
        }
        if (argsStatus == ARGS_TRANSFER_TO_PC) {
            val accountId = Session.getAccountId()
            val extensionSessionId = Session.getExtensionSessionId()
            if (!accountId.isNullOrBlank() && !extensionSessionId.isNullOrBlank()) {
                activeSessionId =
                    transferSessionManager.initiateSession(
                        direction = TransferDirection.TRANSFER_TO_PC,
                        accountId = accountId,
                        expectedExtensionSessionId = extensionSessionId,
                    ).sessionId
            }
        }
        if (argsStatus == ARGS_TRANSFER_TO_PC || argsStatus == ARGS_RESTORE_FROM_PC) {
            RxBus.listen(TransferCommand::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(destroyScope)
                .subscribe { command ->
                    if (command.action == TransferCommandAction.CANCEL.value) {
                        transferSessionManager.cancelSession(activeSessionId)
                        pairingDialog?.dismiss()
                        pairingDialog = null
                        lifecycleScope.launch {
                            if (argsStatus == ARGS_RESTORE_FROM_PC) {
                                if (status.value == TransferStatus.WAITING_MESSAGE) {
                                    status.value = TransferStatus.INITIALIZING
                                } else if (status.value != TransferStatus.FINISHED && status.value != TransferStatus.INITIALIZING) {
                                    transferClient.cancelAndExit()
                                } else {
                                    status.value = TransferStatus.INITIALIZING
                                }
                                toast(R.string.Canceled)
                            } else if (status.value == TransferStatus.WAITING_MESSAGE) {
                                status.value = TransferStatus.INITIALIZING
                                toast(R.string.Canceled)
                            } else if (transferServer.cancelWaiting()) {
                                toast(R.string.Canceled)
                            }
                        }
                    }
                }
        }
    }

    private val argsStatus by lazy {
        intent.getIntExtra(ARGS_STATUS, ARGS_TRANSFER_TO_PHONE)
    }

    private var selectConversation: Set<String>? = null
    private var selectDate: Int? = null

    private fun initView() {
        binding.titleView.isVisible = true
        binding.initLl.isVisible = true
        binding.waitingLl.isVisible = false
        binding.pbFl.isVisible = false
        binding.selectLl.isVisible = true
        binding.conversationRl.setOnClickListener {
            val ft =
                supportFragmentManager.beginTransaction().add(
                    R.id.container,
                    SelectConversationFragment.newInstance().apply {
                        this.callback = { result ->
                            this@TransferActivity.selectConversation = result
                            renderConversation()
                        }
                    },
                    SelectConversationFragment.TAG,
                ).setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
            ft.addToBackStack(null)
            ft.commitAllowingStateLoss()
        }
        binding.dataRl.setOnClickListener {
            val ft =
                supportFragmentManager.beginTransaction().add(
                    R.id.container,
                    SelectDateFragment.newInstance().apply {
                        this.callback = { result ->
                            this@TransferActivity.selectDate = result
                            renderDate()
                        }
                    },
                    SelectDateFragment.TAG,
                ).setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
            ft.addToBackStack(null)
            ft.commitAllowingStateLoss()
        }
        binding.pbTips.isVisible = false
        binding.startTv.setText(R.string.transfer_now)
        binding.startTv.isEnabled = true
        binding.start.isClickable = true
        binding.selectLl.isVisible = argsStatus == ARGS_TRANSFER_TO_PHONE || argsStatus == ARGS_TRANSFER_TO_PC
        binding.start.setOnClickListener {
            if (!this@TransferActivity.isConnectedToWiFi()) {
                alertDialogBuilder()
                    .setTitle(getString(R.string.Make_sure_WiFi))
                    .setPositiveButton(R.string.Confirm) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }
            when (argsStatus) {
                ARGS_TRANSFER_TO_PHONE -> {
                    val accountId = Session.getAccountId() ?: return@setOnClickListener
                    activeSessionId =
                        transferSessionManager.initiateSession(
                            direction = TransferDirection.TRANSFER_TO_PHONE,
                            accountId = accountId,
                        ).sessionId
                    lifecycleScope.launch {
                        transferServer.startServer(selectConversation, selectDate) { transferCommandData ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                val qrCode =
                                    gson.toJson(transferCommandData)
                                        .base64Encode()
                                        .run {
                                            "$DEVICE_TRANSFER?data=$this"
                                        }
                                        .generateQRCode(240.dp).first
                                binding.qr.setImageBitmap(qrCode)
                                binding.qrFl.fadeIn()
                                binding.initLl.isVisible = false
                                binding.waitingLl.isVisible = false
                            }
                        }
                    }
                }

                ARGS_TRANSFER_TO_PC -> {
                    if (this@TransferActivity.status.value != TransferStatus.CREATED) {
                        if (chatWebSocket.connected) {
                            pushRequest()
                        } else {
                            alertDialogBuilder()
                                .setTitle(getString(R.string.Unable_connect_desktop))
                                .setPositiveButton(R.string.Confirm) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                }

                ARGS_RESTORE_FROM_PC -> {
                    if (this@TransferActivity.status.value != TransferStatus.WAITING_MESSAGE) {
                        if (chatWebSocket.connected) {
                            pullRequest()
                        } else {
                            alertDialogBuilder()
                                .setTitle(getString(R.string.Unable_connect_desktop))
                                .setPositiveButton(R.string.Confirm) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .show()
                        }
                    }
                }

                ARGS_RESTORE_FROM_PHONE -> {
                    RxPermissions(this)
                        .request(Manifest.permission.CAMERA)
                        .autoDispose(stopScope)
                        .subscribe { granted ->
                            if (granted) {
                                val accountId = Session.getAccountId() ?: return@subscribe
                                activeSessionId =
                                    transferSessionManager.initiateSession(
                                        direction = TransferDirection.RESTORE_FROM_PHONE,
                                        accountId = accountId,
                                    ).sessionId
                                getScanResult.launch(
                                    Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true),
                                )
                            } else {
                                openPermissionSetting()
                            }
                        }
                }
            }
        }
        when (argsStatus) {
            ARGS_TRANSFER_TO_PHONE -> {
                binding.titleView.setSubTitle(getString(R.string.Transfer_to_Another_Phone), "")
                binding.logoIv.setImageResource(R.drawable.ic_transfer)
                binding.progressIv.setImageResource(R.drawable.ic_transfer)
                binding.initDesc.setText(R.string.transfer_phone_desc)
            }

            ARGS_TRANSFER_TO_PC -> {
                binding.titleView.setSubTitle(getString(R.string.Transfer_to_PC), "")
                binding.logoIv.setImageResource(R.drawable.ic_transfer_to_pc)
                binding.progressIv.setImageResource(R.drawable.ic_transfer_to_pc)
                binding.initDesc.setText(R.string.transfer_pc_desc)
            }

            ARGS_RESTORE_FROM_PC -> {
                binding.titleView.setSubTitle(getString(R.string.Restore_from_PC), "")
                binding.logoIv.setImageResource(R.drawable.ic_restore_from_pc)
                binding.progressIv.setImageResource(R.drawable.ic_restore_from_pc)
                binding.initDesc.setText(R.string.restore_desc)
            }

            ARGS_RESTORE_FROM_PHONE -> {
                binding.titleView.setSubTitle(getString(R.string.Restore_from_Another_Phone), "")
                binding.logoIv.setImageResource(R.drawable.ic_transfer)
                binding.progressIv.setImageResource(R.drawable.ic_transfer)
                binding.initDesc.isVisible = false
                binding.initScanDesc.isVisible = true
                binding.startTv.setText(R.string.Scane_to_Restore)
            }
        }
    }

    private fun renderConversation() {
        if (selectConversation.isNullOrEmpty()) {
            binding.conversationTv.setText(R.string.All_Conversations)
        } else {
            binding.conversationTv.text = getString(R.string.Chats, selectConversation?.size ?: 0)
        }
    }

    private fun renderDate() {
        val dateOffset = selectDate
        if (dateOffset == null || dateOffset == 0) {
            binding.dateTv.setText(R.string.all_time)
        } else {
            // Maybe consider the local calendar for a year instead of 12 months.
            if (dateOffset % 12 == 0) {
                // year
                binding.dateTv.text =
                    resources.getQuantityString(
                        R.plurals.last_year,
                        if (dateOffset / 12 > 1) {
                            0
                        } else {
                            1
                        },
                        dateOffset / 12,
                    )
                Timber.e(binding.dateTv.text.toString())
            } else {
                // month
                binding.dateTv.text =
                    resources.getQuantityString(
                        R.plurals.last_month,
                        if (dateOffset > 1) {
                            0
                        } else {
                            1
                        },
                        dateOffset,
                    )
                Timber.e(binding.dateTv.text.toString())
            }
        }
    }

    private var transferDisposable: Disposable? = null
    private var transferSpeedDisposable: Disposable? = null
    private var transferControlDisposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (transferDisposable == null) {
            transferDisposable =
                RxBus.listen(DeviceTransferProgressEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe {
                        binding.progress.progress = it.progress.toInt()
                        if (status.value == TransferStatus.PROCESSING) {
                            binding.pbTv.text = getString(R.string.transfer_process_desc, String.format("%.2f%%", it.progress))
                        } else if (status.value == TransferStatus.SYNCING) {
                            binding.progressTv.text = getString(R.string.transferring_chat_progress, String.format("%.2f%%", it.progress))
                        }
                    }
        }

        if (transferSpeedDisposable == null) {
            transferSpeedDisposable =
                RxBus.listen(SpeedEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe {
                        if (status.value == TransferStatus.SYNCING) {
                            binding.pbTv.text = it.speed
                        }
                    }
        }
        if (transferControlDisposable == null) {
            transferControlDisposable =
                RxBus.listen(IncomingTransferControlEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe { event ->
                        handleIncomingControlEvent(event)
                    }
        }
    }

    private fun handleIncomingControlEvent(event: IncomingTransferControlEvent) {
        val command = event.command
        val accountId = Session.getAccountId() ?: return
        val sessionId = activeSessionId ?: return
        when (argsStatus) {
            ARGS_RESTORE_FROM_PC -> {
                if (command.action != TransferCommandAction.PUSH.value) return
                if (!validateIncomingPushCommand(command, accountId, requireDeviceId = false)) return
                val remoteDeviceId: String? = command.deviceId
                if (!transferSessionManager.beginPairingConfirmation(sessionId, accountId, remoteDeviceId, command.code)) {
                    toast(R.string.transfer_pairing_expired_or_cancelled)
                    status.value = TransferStatus.INITIALIZING
                    return
                }
                showPairingConfirmationDialog(command, sessionId)
            }

            ARGS_TRANSFER_TO_PC -> {
                if (command.action != TransferCommandAction.PULL.value) return
                val remoteDeviceId: String? = command.deviceId
                if (!transferSessionManager.beginPairingConfirmation(sessionId, accountId, remoteDeviceId, command.code)) {
                    toast(R.string.transfer_pairing_expired_or_cancelled)
                    return
                }
                showPushApprovalDialog(command, sessionId)
            }
        }
    }

    private suspend fun connect(
        transferCommandData: TransferCommand,
        authorization: TransferAuthorization,
        onConnectionFailed: () -> Unit = {},
    ) {
        val ip = requireNotNull(transferCommandData.ip)
        val port = requireNotNull(transferCommandData.port)
        val key = requireNotNull(transferCommandData.secretKey).decodeBase64()
        connect(
            ip,
            port,
            TransferCommand(
                TransferCommandAction.CONNECT.value,
                code = transferCommandData.code,
                userId = Session.getAccountId(),
            ),
            key = key,
            authorization = authorization,
            onConnectionFailed = onConnectionFailed,
        )
    }

    private suspend fun connect(
        ip: String,
        port: Int,
        transferCommand: TransferCommand,
        key: ByteArray,
        authorization: TransferAuthorization,
        onConnectionFailed: () -> Unit = {},
    ) {
        transferClient.connectToServer(ip, port, transferCommand, key, authorization, onConnectionFailed)
    }

    override fun onStop() {
        super.onStop()
        transferDisposable?.dispose()
        transferSpeedDisposable?.dispose()
        transferControlDisposable?.dispose()
        transferDisposable = null
        transferSpeedDisposable = null
        transferControlDisposable = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        dialog?.dismiss()
        pairingDialog?.dismiss()
        pairingDialog = null
        if (status.value != TransferStatus.FINISHED) {
            transferSessionManager.cancelSession(activeSessionId)
            transferClient.cancelAndExit()
        } else {
            transferClient.exit(true)
        }
        transferServer.exit()
        status.value = TransferStatus.INITIALIZING
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (shouldLogout) {
            MixinApplication.get().closeAndClear()
        }
    }

    private fun handleQrCodeContent(content: String) =
        lifecycleScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable)
                lifecycleScope.launch(Dispatchers.Main) {
                    toast(R.string.Data_error)
                }
            },
        ) {
            val transferCommandData =
                try {
                    gson.fromJson(
                        String(content.base64RawURLDecode()),
                        TransferCommand::class.java,
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toast(R.string.Data_error)
                    }
                    return@launch
                }
            val accountId = Session.getAccountId()
            val sessionId = activeSessionId
            if (accountId.isNullOrBlank() || sessionId.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    toast(R.string.transfer_pairing_expired_or_cancelled)
                }
                return@launch
            }
            if (transferCommandData.userId != accountId) {
                withContext(Dispatchers.Main) {
                    toast(R.string.not_yours)
                    transferSessionManager.cancelSession(sessionId)
                    finish()
                }
                return@launch
            }
            if (transferCommandData.version > CURRENT_TRANSFER_VERSION) {
                withContext(Dispatchers.Main) {
                    toast(R.string.Version_not_supported)
                    transferSessionManager.cancelSession(sessionId)
                    finish()
                }
                return@launch
            }
            if (!validateIncomingPushCommand(transferCommandData, accountId, requireDeviceId = true)) {
                return@launch
            }
            val remoteDeviceId: String? = transferCommandData.deviceId
            if (!transferSessionManager.beginPairingConfirmation(sessionId, accountId, remoteDeviceId, transferCommandData.code)) {
                withContext(Dispatchers.Main) {
                    toast(R.string.transfer_pairing_expired_or_cancelled)
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                showPairingConfirmationDialog(transferCommandData, sessionId)
            }
        }

    private fun validateIncomingPushCommand(
        command: TransferCommand,
        accountId: String,
        requireDeviceId: Boolean,
    ): Boolean {
        if (command.action != TransferCommandAction.PUSH.value) {
            toast(R.string.Data_error)
            return false
        }
        if (!command.userId.isNullOrBlank() && command.userId != accountId) {
            toast(R.string.not_yours)
            return false
        }
        if (command.version > CURRENT_TRANSFER_VERSION) {
            toast(R.string.Version_not_supported)
            return false
        }
        val port = command.port
        val remoteDeviceId: String? = command.deviceId
        if (command.ip.isNullOrBlank() ||
            port == null ||
            port !in 1..65535 ||
            command.secretKey.isNullOrBlank() ||
            command.code == null ||
            (requireDeviceId && remoteDeviceId.isNullOrBlank())
        ) {
            toast(R.string.Data_error)
            return false
        }
        return true
    }

    private fun showPairingConfirmationDialog(
        command: TransferCommand,
        sessionId: String,
    ) {
        if (isFinishing || isDestroyed) return
        pairingDialog?.dismiss()
        val platformLabel: String = (command.platform as String?)?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
        val endpointLabel =
            command.code?.let { "#$it" }
                ?: command.ip.orEmpty()
        pairingDialog =
            alertDialogBuilder()
                .setTitle(R.string.transfer_pairing_confirm_title)
                .setMessage(getString(R.string.transfer_pairing_confirm_message, platformLabel, endpointLabel))
                .setCancelable(false)
                .setNegativeButton(R.string.Cancel) { dialog, _ ->
                    dialog.dismiss()
                    pairingDialog = null
                    transferSessionManager.cancelSession(sessionId)
                    status.value = TransferStatus.INITIALIZING
                }
                .setPositiveButton(R.string.Confirm) { dialog, _ ->
                    dialog.dismiss()
                    pairingDialog = null
                    val remoteDeviceId: String? = command.deviceId
                    val authorization =
                        transferSessionManager.confirmAndConsumePairing(
                            sessionId = sessionId,
                            accountId = Session.getAccountId(),
                            peerDeviceId = remoteDeviceId,
                        )
                    if (authorization == null) {
                        toast(R.string.transfer_pairing_expired_or_cancelled)
                        status.value = TransferStatus.INITIALIZING
                        return@setPositiveButton
                    }
                    lifecycleScope.launch {
                        connect(command, authorization) {
                            if (argsStatus == ARGS_RESTORE_FROM_PC) {
                                sendMessage(gson.toJson(TransferCommand(TransferCommandAction.CANCEL.value)))
                            }
                        }
                    }
                }
                .create()
        pairingDialog?.show()
    }

    private fun showPushApprovalDialog(
        command: TransferCommand,
        sessionId: String,
    ) {
        if (isFinishing || isDestroyed) return
        pairingDialog?.dismiss()
        val platformLabel: String = (command.platform as String?)?.takeIf { it.isNotBlank() } ?: "Desktop"
        val deviceLabel: String = (command.deviceId as String?)?.takeIf { it.isNotBlank() } ?: platformLabel
        pairingDialog =
            alertDialogBuilder()
                .setTitle(R.string.transfer_pairing_confirm_title)
                .setMessage(getString(R.string.transfer_pairing_confirm_message, platformLabel, deviceLabel))
                .setCancelable(false)
                .setNegativeButton(R.string.Cancel) { dialog, _ ->
                    dialog.dismiss()
                    pairingDialog = null
                    transferSessionManager.cancelSession(sessionId)
                }
                .setPositiveButton(R.string.Confirm) { dialog, _ ->
                    dialog.dismiss()
                    pairingDialog = null
                    val remoteDeviceId: String? = command.deviceId
                    val authorization =
                        transferSessionManager.confirmAndConsumePairing(
                            sessionId = sessionId,
                            accountId = Session.getAccountId(),
                            peerDeviceId = remoteDeviceId,
                        )
                    if (authorization == null) {
                        toast(R.string.transfer_pairing_expired_or_cancelled)
                        return@setPositiveButton
                    }
                    pushRequest()
                }
                .create()
        pairingDialog?.show()
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var participantDao: ParticipantDao

    private fun sendMessage(content: String) =
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accountId = Session.getAccountId() ?: return@launch
                val sessionId = Session.getExtensionSessionId() ?: return@launch
                val identityNumber = Session.getAccount()?.identityNumber ?: return@launch
                val plainText =
                    gson.toJson(
                        PlainJsonMessagePayload(
                            action = PlainDataAction.DEVICE_TRANSFER.name,
                            content = content,
                        ),
                    )
                val encoded = plainText.toByteArray().base64RawURLEncode()
                val bm =
                    createParamBlazeMessage(
                        createPlainJsonParam(
                            MixinDatabase.getDatabase(this@TransferActivity, identityNumber).participantDao()
                                .joinedConversationId(accountId) ?: generateConversationId(accountId, Constants.TEAM_MIXIN_USER_ID),
                            accountId,
                            encoded,
                            sessionId,
                        ),
                    )
                jobManager.addJobInBackground(SendPlaintextJob(bm, BaseJob.PRIORITY_UI_HIGH))
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

    private fun pullRequest() {
        val accountId = Session.getAccountId() ?: return
        val extensionSessionId = Session.getExtensionSessionId()
        if (extensionSessionId.isNullOrBlank()) {
            toast(R.string.Please_log_in_to_the_desktop_first)
            return
        }
        activeSessionId =
            transferSessionManager.initiateSession(
                direction = TransferDirection.RESTORE_FROM_PC,
                accountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ).sessionId
        val encodeText =
            gson.toJson(
                TransferCommand(
                    TransferCommandAction.PULL.value,
                ),
            )
        status.value = TransferStatus.WAITING_MESSAGE
        sendMessage(encodeText)
    }

    private fun pushRequest() {
        val accountId = Session.getAccountId() ?: return
        val extensionSessionId = Session.getExtensionSessionId()
        if (extensionSessionId.isNullOrBlank()) {
            toast(R.string.Please_log_in_to_the_desktop_first)
            return
        }
        activeSessionId =
            transferSessionManager.initiateSession(
                direction = TransferDirection.TRANSFER_TO_PC,
                accountId = accountId,
                expectedExtensionSessionId = extensionSessionId,
            ).sessionId
        lifecycleScope.launch {
            transferServer.startServer(selectConversation, selectDate) { transferData ->
                val encodeText =
                    gson.toJson(
                        transferData,
                    )
                sendMessage(encodeText)
            }
        }
    }
}

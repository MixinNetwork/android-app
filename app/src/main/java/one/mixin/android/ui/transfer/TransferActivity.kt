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
import one.mixin.android.extension.getParcelableExtraCompat
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
        const val ARGS_QR_CODE_CONTENT = "args_qr_code_content"
        const val ARGS_COMMAND = "args_command"

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

        fun show(
            context: Context,
            qrCodeContent: String,
        ) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_RESTORE_FROM_PHONE)
                    putExtra(ARGS_QR_CODE_CONTENT, qrCodeContent)
                },
            )
        }

        fun show(
            context: Context,
            transferCommandData: TransferCommand,
        ) {
            val status =
                if (transferCommandData.action == TransferCommandAction.PULL.value) {
                    ARGS_RESTORE_FROM_PC
                } else {
                    if (transferCommandData.action == TransferCommandAction.PUSH.value) {
                        ARGS_TRANSFER_TO_PC
                    } else {
                        null
                    } ?: return
                }
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, status)
                    putExtra(ARGS_COMMAND, transferCommandData)
                },
            )
        }

        fun parseUri(
            context: Context,
            uri: Uri,
            success: (() -> Unit)? = null,
            fallback: () -> Unit,
        ) {
            val data = uri.getQueryParameter("data")
            if (data == null) {
                fallback.invoke()
                return
            }
            show(context, data)
            success?.invoke()
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
        val text = data?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT) ?: return
        val content =
            try {
                Uri.parse(text).getQueryParameter("data")
            } catch (e: Exception) {
                toast(R.string.Data_error)
                null
            } ?: return
        connectToQrCodeContent(content)
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
        intent.getParcelableExtraCompat(ARGS_COMMAND, TransferCommand::class.java)?.apply {
            handleCommand(this)
        }
        intent.getStringExtra(ARGS_QR_CODE_CONTENT)?.let { qrCodeContent ->
            connectToQrCodeContent(qrCodeContent)
        }
    }

    private val argsStatus by lazy {
        intent.getIntExtra(ARGS_STATUS, ARGS_TRANSFER_TO_PHONE)
    }

    private var selectConversation: Set<String>? = null
    private var selectDate: Int? = null

    private fun initView() {
        binding.titleView.isVisible = true
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let {
            it.getParcelableExtraCompat(ARGS_COMMAND, TransferCommand::class.java)?.apply {
                handleCommand(this)
            }
        }
    }

    private var transferDisposable: Disposable? = null
    private var transferSpeedDisposable: Disposable? = null
    private var transferCommandDisposable: Disposable? = null

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

        if (transferCommandDisposable == null) {
            transferCommandDisposable =
                RxBus.listen(TransferCommand::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(destroyScope)
                    .subscribe {
                        if (argsStatus == ARGS_TRANSFER_TO_PC || argsStatus == ARGS_RESTORE_FROM_PC) {
                            if (it.action == TransferCommandAction.CANCEL.value) {
                                status.value = TransferStatus.INITIALIZING
                            }
                        }
                    }
        }
    }

    private fun handleCommand(transferCommandData: TransferCommand) {
        when (transferCommandData.action) {
            TransferCommandAction.PUSH.value -> {
                lifecycleScope.launch {
                    connect(transferCommandData)
                }
            }

            TransferCommandAction.PULL.value -> {
                pushRequest()
            }

            else -> {}
        }
    }

    private suspend fun connect(transferCommandData: TransferCommand) {
        val ip = requireNotNull(transferCommandData.ip)
        val port = requireNotNull(transferCommandData.port)
        val key = requireNotNull(transferCommandData.secretKey).decodeBase64()
        connect(ip, port, TransferCommand(TransferCommandAction.CONNECT.value, code = transferCommandData.code, userId = Session.getAccountId()), key = key)
    }

    private suspend fun connect(
        ip: String,
        port: Int,
        transferCommand: TransferCommand,
        key: ByteArray,
    ) {
        transferClient.connectToServer(ip, port, transferCommand, key)
    }

    override fun onStop() {
        super.onStop()
        transferDisposable?.dispose()
        transferSpeedDisposable?.dispose()
        transferCommandDisposable?.dispose()
        transferDisposable = null
        transferSpeedDisposable = null
        transferCommandDisposable = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        dialog?.dismiss()
        transferServer.exit()
        transferClient.exit()
        status.value = TransferStatus.INITIALIZING
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (shouldLogout) {
            MixinApplication.get().closeAndClear()
        }
    }

    private fun connectToQrCodeContent(content: String) =
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
            Timber.e("qrcode:$content")
            if (transferCommandData.userId != Session.getAccountId()) {
                toast(R.string.not_yours)
                finish()
                return@launch
            }
            if (transferCommandData.version > CURRENT_TRANSFER_VERSION) {
                toast(R.string.Version_not_supported)
                finish()
                return@launch
            }
            connect(transferCommandData)
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
                            MixinDatabase.getDatabase(this@TransferActivity).participantDao()
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
        val encodeText =
            gson.toJson(
                TransferCommand(
                    TransferCommandAction.PULL.value,
                ).apply {
                    Timber.e("pull ${gson.toJson(this)}")
                },
            )
        status.value = TransferStatus.WAITING_MESSAGE
        sendMessage(encodeText)
    }

    private fun pushRequest() {
        lifecycleScope.launch {
            transferServer.startServer(selectConversation, selectDate) { transferData ->
                Timber.e("push ${gson.toJson(transferData)}")
                val encodeText =
                    gson.toJson(
                        transferData,
                    )
                sendMessage(encodeText)
            }
        }
    }
}

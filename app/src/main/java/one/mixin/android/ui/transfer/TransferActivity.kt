package one.mixin.android.ui.transfer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getParcelableExtra
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.job.BaseJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendPlaintextJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.landing.InitializeActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.viewBinding
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import timber.log.Timber
import javax.inject.Inject

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

        fun show(context: Context, qrCodeContent: String) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ARGS_STATUS, ARGS_RESTORE_FROM_PHONE)
                    putExtra(ARGS_QR_CODE_CONTENT, qrCodeContent)
                },
            )
        }

        fun show(context: Context, transferCommandData: TransferCommandData) {
            val status = if (transferCommandData.action == TransferCommandAction.PULL.value) {
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

    var shouldLogout = false

    override fun onBackPressed() {
        if (status.value in listOf(
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
        val content = try {
            Uri.parse(text).getQueryParameter("data")
        } catch (e: Exception) {
            toast(R.string.Data_error)
            null
        } ?: return
        connectToQrCodeContent(content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        getScanResult = registerForActivityResult(
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
                }

                TransferStatus.CONNECTING -> {
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.WAITING_FOR_VERIFICATION -> {
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.VERIFICATION_COMPLETED -> {
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                    binding.pbFl.isVisible = true
                    binding.pbTips.isVisible = true
                }

                TransferStatus.SENDING -> {
                    binding.titleView.isVisible = false
                    binding.qrFl.isVisible = false
                    binding.initLl.isVisible = false
                    binding.waitingLl.isVisible = true
                }

                TransferStatus.ERROR -> {
                    alertDialogBuilder()
                        .setTitle(R.string.Transfer_error)
                        .setCancelable(false)
                        .setPositiveButton(R.string.Confirm) { dialog, _ ->
                            dialog.dismiss()
                            finish()
                            status.value = TransferStatus.INITIALIZING
                        }
                        .show()
                }

                TransferStatus.FINISHED -> {
                    alertDialogBuilder()
                        .setTitle(R.string.Transfer_completed)
                        .setCancelable(false)
                        .setPositiveButton(R.string.Confirm) { dialog, _ ->
                            dialog.dismiss()
                            if (argsStatus == ARGS_RESTORE_FROM_PHONE || argsStatus == ARGS_RESTORE_FROM_PC) {
                                InitializeActivity.showLoading(this)
                            }
                            finish()
                            status.value = TransferStatus.INITIALIZING
                        }
                        .show()
                }
            }
        }
        getParcelableExtra(intent, ARGS_COMMAND, TransferCommandData::class.java)?.apply {
            handleCommand(this)
        }
        intent.getStringExtra(ARGS_QR_CODE_CONTENT)?.let { qrCodeContent ->
            connectToQrCodeContent(qrCodeContent)
        }
    }

    private val argsStatus by lazy {
        intent.getIntExtra(ARGS_STATUS, ARGS_TRANSFER_TO_PHONE)
    }

    private fun initView() {
        binding.titleView.isVisible = true
        binding.pbFl.isVisible = false
        binding.pbTips.isVisible = false
        binding.startTv.setText(R.string.transfer_now)
        binding.startTv.isEnabled = true
        binding.start.isClickable = true
        binding.start.setOnClickListener {
            when (argsStatus) {
                ARGS_TRANSFER_TO_PHONE -> {
                    lifecycleScope.launch {
                        transferServer.startServer { transferCommandData ->
                            lifecycleScope.launch(Dispatchers.Main) {
                                val qrCode = gson.toJson(transferCommandData)
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
                        pushRequest()
                    }
                }

                ARGS_RESTORE_FROM_PC -> {
                    if (this@TransferActivity.status.value != TransferStatus.WAITING_MESSAGE) {
                        pullRequest()
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            getParcelableExtra(it, ARGS_COMMAND, TransferCommandData::class.java)?.apply {
                handleCommand(this)
            }
        }
    }

    private var transferDisposable: Disposable? = null
    private var transferSpeedDisposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (transferDisposable == null) {
            transferDisposable = RxBus.listen(DeviceTransferProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(destroyScope)
                .subscribe {
                    binding.progressTv.text =
                        getString(R.string.sending_desc, String.format("%.1f%%", it.progress))
                    Timber.e("Device transfer ${it.progress}%")
                }
        }

        if (transferSpeedDisposable == null) {
            transferSpeedDisposable = RxBus.listen(SpeedEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(destroyScope)
                .subscribe {
                    Timber.e("Speed: ${it.speed}")
                }
        }
    }

    private fun handleCommand(commandData: TransferCommandData) {
        when (commandData.action) {
            TransferCommandAction.PUSH.value -> {
                lifecycleScope.launch {
                    transferClient.connectToServer(
                        commandData.ip!!,
                        commandData.port!!,
                        TransferCommandData(
                            TransferCommandAction.CONNECT.value,
                            code = commandData.code,
                        ),
                    )
                }
            }

            TransferCommandAction.PULL.value -> {
                pushRequest()
            }

            else -> {}
        }
    }

    override fun onStop() {
        super.onStop()
        transferDisposable?.dispose()
        transferSpeedDisposable?.dispose()
        transferDisposable = null
        transferSpeedDisposable = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
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
            val transferCommandData = try {
                gson.fromJson(
                    String(content.base64RawURLDecode()),
                    TransferCommandData::class.java,
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
            }
            transferClient.connectToServer(
                transferCommandData.ip!!,
                transferCommandData.port!!,
                TransferCommandData(
                    TransferCommandAction.CONNECT.value,
                    code = transferCommandData.code,
                    userId = Session.getAccountId(),
                ),
            )
        }

    private val gson by lazy {
        GsonHelper.customGson
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var participantDao: ParticipantDao

    private fun sendMessage(content: String) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val accountId = Session.getAccountId() ?: return@launch
            val sessionId = Session.getExtensionSessionId() ?: return@launch
            val plainText = gson.toJson(
                PlainJsonMessagePayload(
                    action = PlainDataAction.DEVICE_TRANSFER.name,
                    content = content,
                ),
            )
            val encoded = plainText.toByteArray().base64RawURLEncode()
            val bm = createParamBlazeMessage(
                createPlainJsonParam(
                    MixinDatabase.getDatabase(this@TransferActivity).participantDao()
                        .joinedConversationId(accountId),
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
        val encodeText = gson.toJson(
            TransferCommandData(
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
            transferServer.startServer { transferData ->
                Timber.e("push ${gson.toJson(transferData)}")
                val encodeText = gson.toJson(
                    transferData,
                )
                sendMessage(encodeText)
            }
        }
    }
}

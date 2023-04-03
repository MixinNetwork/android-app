package one.mixin.android.ui.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import one.mixin.android.Constants.Scheme.DEVICE_TRANSFER
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.event.DeviceTransferProgressEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getParcelableExtra
import one.mixin.android.extension.toast
import one.mixin.android.job.BaseJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendPlaintextJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
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
        const val ARGS_IS_COMPUTER = "args_is_computer"
        const val ARGS_QR_CODE_CONTENT = "args_qr_code_content"
        const val ARGS_COMMAND = "args_command"

        fun show(context: Context, isComputer: Boolean, qrCodeContent: String? = null) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    putExtra(ARGS_IS_COMPUTER, isComputer)
                    putExtra(ARGS_QR_CODE_CONTENT, qrCodeContent)
                },
            )
        }

        fun show(context: Context, transferCommandData: TransferCommandData, isComputer: Boolean) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    putExtra(ARGS_IS_COMPUTER, isComputer)
                    putExtra(ARGS_COMMAND, transferCommandData)
                },
            )
        }

        fun parseUri(
            context: Context,
            isComputer: Boolean,
            uri: Uri,
            success: (() -> Unit)? = null,
            fallback: () -> Unit,
        ) {
            val data = uri.getQueryParameter("data")
            if (data == null) {
                fallback.invoke()
                return
            }
            show(context, isComputer, data)
            success?.invoke()
        }
    }

    private val binding by viewBinding(ActivityTransferBinding::inflate)

    @Inject
    lateinit var transferServer: TransferServer

    @Inject
    lateinit var transferClient: TransferClient

    @Inject
    lateinit var status: TransferStatusLiveData

    var shouldLogout = false

    override fun onBackPressed() {
        if (status.value in listOf(TransferStatus.INITIALIZING, TransferStatus.ERROR, TransferStatus.FINISHED)) {
            super.onBackPressed()
        } else {
            toast(R.string.cannot_exit_in_backup)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.titleView.leftIb.setOnClickListener {
            finish()
        }
        val isComputer = intent.getBooleanExtra(ARGS_IS_COMPUTER, false)
        binding.startServer.isVisible = !isComputer
        binding.pullFromDesktop.isVisible = isComputer
        binding.pushToDesktop.isVisible = isComputer
        binding.startServer.setOnClickListener {
            lifecycleScope.launch {
                transferServer.startServer { transferCommandData ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val qrCode = gson.toJson(transferCommandData)
                            .base64Encode()
                            .run {
                                "$DEVICE_TRANSFER?data=$this"
                            }
                            .generateQRCode(240.dp).first
                        toast("Sever IP: ${transferCommandData.ip} ${transferCommandData.action}")
                        binding.startServer.isVisible = false
                        binding.pushToDesktop.isVisible = false
                        binding.pullFromDesktop.isVisible = false
                        binding.qr.setImageBitmap(qrCode)
                        binding.qrFl.fadeIn()
                        binding.loginScanTv.fadeIn()
                    }
                }
            }
        }

        binding.pushToDesktop.setOnClickListener {
            pushRequest()
        }
        binding.pullFromDesktop.setOnClickListener {
            pullRequest()
        }

        intent.getStringExtra(ARGS_QR_CODE_CONTENT)?.let { qrCodeContent ->
            connectToQrCodeContent(qrCodeContent)
        }

        status.observe(this) { s ->
            binding.startServer.isVisible = false
            binding.pushToDesktop.isVisible = false
            binding.pullFromDesktop.isVisible = false

            binding.statusTv.text = s.name
            when (s) {
                TransferStatus.INITIALIZING -> {
                    binding.qrFl.isVisible = false
                    binding.loginScanTv.isVisible = false
                    binding.statusLl.isVisible = false
                }

                TransferStatus.CREATED -> {
                }

                TransferStatus.WAITING_FOR_CONNECTION -> {
                }

                TransferStatus.CONNECTING -> {
                }

                TransferStatus.WAITING_FOR_VERIFICATION -> {
                }

                TransferStatus.VERIFICATION_COMPLETED -> {
                }

                TransferStatus.SENDING -> {
                    binding.qrFl.isVisible = false
                    binding.loginScanTv.isVisible = false
                    binding.statusLl.isVisible = true
                }

                TransferStatus.ERROR -> {
                    binding.qrFl.isVisible = true
                    binding.loginScanTv.isVisible = true
                    binding.statusLl.isVisible = false
                    binding.statusTv.text = getString(R.string.Network_error)
                    status.value = TransferStatus.INITIALIZING
                    toast("出错") // Todo replace
                    finish()
                }

                TransferStatus.FINISHED -> {
                    binding.statusTv.text = getString(R.string.Diagnosis_Complete)
                    toast(R.string.Backup_success) // todo replace string
                    finish()
                }
            }
        }
        getParcelableExtra(intent, ARGS_COMMAND, TransferCommandData::class.java)?.apply {
            handleCommand(this)
        }
    }

    private var disposable: Disposable? = null
    private var transferDisposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        if (disposable == null) {
            disposable = RxBus.listen(TransferCommandData::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe {
                    Timber.e("Rx ${gson.toJson(it)}")
                    handleCommand(it)
                }
        }
        if (transferDisposable == null) {
            transferDisposable = RxBus.listen(DeviceTransferProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe {
                    if (status.value == TransferStatus.SENDING) {
                        binding.descTv.text = getString(R.string.sending_desc, String.format("%.2f%%", it.progress))
                        binding.pb.max = 100
                        binding.pb.progress = 40
                    }
                    Timber.e("Device transfer ${it.progress}%")
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
                // Todo display loading and delayed shutdown
                // Wait for the server push
                pushRequest()
            }

            else -> {}
        }
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
        transferDisposable?.dispose()
        transferDisposable = null
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
                lifecycleScope.launch(Dispatchers.Main) {
                    toast(R.string.Data_error)
                }
            },
        ) {
            withContext(Dispatchers.Main) {
                binding.startServer.isVisible = false
            }

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
            // Todo
            // if (transferCommandData.userId != Session.getAccountId()){
            //     finish()
            // }
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

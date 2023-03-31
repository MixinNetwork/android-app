package one.mixin.android.ui.transfer

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.job.BaseJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendPlaintextJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.transfer.vo.TransferCommandAction
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.ui.transfer.vo.TransferStatus
import one.mixin.android.ui.transfer.vo.TransferStatusLiveData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_SOCKET_THREAD
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

        fun show(context: Context, isComputer: Boolean, qrCodeContent: String? = null) {
            context.startActivity(
                Intent(context, TransferActivity::class.java).apply {
                    putExtra(ARGS_IS_COMPUTER, isComputer)
                    putExtra(ARGS_QR_CODE_CONTENT, qrCodeContent)
                },
            )
        }

        fun parseUri(context: Context, isComputer: Boolean, uri: Uri, success: (() -> Unit)? = null, fallback: () -> Unit) {
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

    var shouldLogout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract(),
            activityResultRegistry,
            ::callbackScan,
        )
        setContentView(binding.root)
        binding.titleView.leftIb.setOnClickListener {
            finish()
        }
        val isComputer = intent.getBooleanExtra(ARGS_IS_COMPUTER, false)
        binding.clientScan.isVisible = !isComputer
        binding.startServer.isVisible = !isComputer
        binding.pullFromDesktop.isVisible = isComputer
        binding.pushToDesktop.isVisible = isComputer
        binding.startServer.setOnClickListener {
            lifecycleScope.launch(SINGLE_SOCKET_THREAD) {
                transferServer.startServer { transferCommandData ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val qrCode = gson.toJson(transferCommandData)
                            .base64Encode()
                            .generateQRCode(240.dp).first
                        toast("Sever IP: ${transferCommandData.ip} ${transferCommandData.action}")
                        binding.startServer.isVisible = false
                        binding.clientScan.isVisible = false
                        binding.pushToDesktop.isVisible = false
                        binding.pullFromDesktop.isVisible = false
                        binding.qr.setImageBitmap(qrCode)
                        binding.qrFl.fadeIn()
                        binding.loginScanTv.fadeIn()
                    }
                }
            }
        }

        binding.clientScan.setOnClickListener {
            handleClick()
        }
        binding.pushToDesktop.setOnClickListener {
            // loading()
            pushRequest()
        }
        binding.pullFromDesktop.setOnClickListener {
            // loading()
            pullRequest()
        }

        intent.getStringExtra(ARGS_QR_CODE_CONTENT)?.let { qrCodeContent ->
            connectToQrCodeContent(qrCodeContent)
        }

        status.observe(this) { s ->
            binding.startServer.isVisible = false
            binding.clientScan.isVisible = false
            binding.pushToDesktop.isVisible = false
            binding.pullFromDesktop.isVisible = false

            binding.statusTv.text = s.name
            when (s) {
                TransferStatus.INITIALIZING -> {
                }
                TransferStatus.CREATED -> {
                }
                TransferStatus.CONNECTING -> {
                }
                TransferStatus.WAITING_FOR_CONNECTION -> {
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
                }
                TransferStatus.FINISHED -> {
                    binding.statusTv.text = getString(R.string.Diagnosis_Complete)
                }
            }
        }
    }

    private var pb: Dialog? = null
    private fun loading() {
        pb = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
    }

    private fun loadingDismiss() {
        pb?.dismiss()
        pb = null
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
                    when (it.action) {
                        TransferCommandAction.CONNECT.value -> {
                            // This message is received from websocket
                        }

                        TransferCommandAction.PUSH.value -> {
                            loadingDismiss()
                            lifecycleScope.launch(SINGLE_SOCKET_THREAD) {
                                transferClient.connectToServer(
                                    it.ip!!,
                                    it.port!!,
                                    TransferCommandData(
                                        TransferCommandAction.CONNECT.value,
                                        code = it.code,
                                    ),
                                )
                            }
                        }

                        TransferCommandAction.PULL.value -> {
                            // Todo display loading and delayed shutdown
                            // Wait for the server push
                            loading()
                        }

                        else -> {}
                    }
                    Timber.e("${it.action} ${it.deviceId} ${it.ip}")
                }
        }
        if (transferDisposable == null) {
            transferDisposable = RxBus.listen(DeviceTransferProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe {
                    if (status.value == TransferStatus.SENDING) {
                        binding.descTv.text = getString(R.string.sending_desc, it.progress.toString())
                    }
                    Timber.e("Device transfer ${it.progress}%")
                }
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
        MixinApplication.get().applicationScope.launch(SINGLE_SOCKET_THREAD) {
            transferServer.exit()
            transferClient.exit()
        }
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (shouldLogout) {
            MixinApplication.get().closeAndClear()
        }
    }

    private lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>
    private fun handleClick() {
        RxPermissions(this)
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true))
                } else {
                    openPermissionSetting()
                }
            }
    }

    private fun connectToQrCodeContent(content: String) = lifecycleScope.launch(SINGLE_SOCKET_THREAD) {
        withContext(Dispatchers.Main) {
            binding.startServer.isVisible = false
            binding.clientScan.isVisible = false
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
        transferClient.connectToServer(
            transferCommandData.ip!!,
            transferCommandData.port!!,
            TransferCommandData(
                TransferCommandAction.CONNECT.value,
                code = transferCommandData.code,
            ),
        )
    }

    private fun callbackScan(data: Intent?) {
        val qrContent = data?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
        qrContent?.let { content ->
            connectToQrCodeContent(content)
        }
    }

    private val finishListener: (String) -> Unit = { msg ->
        lifecycleScope.launch(Dispatchers.Main) {
            toast(msg)
            binding.startServer.isVisible = true
            binding.clientScan.isVisible = true
            binding.pushToDesktop.isVisible = true
            binding.pullFromDesktop.isVisible = true
            binding.qr.isVisible = false
        }
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    @Inject
    lateinit var participantDao: ParticipantDao

    private fun sendMessage(content: String) {
        lifecycleScope.launch(SINGLE_SOCKET_THREAD) {
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
                jobManager.addJobInBackground(SendPlaintextJob(bm, BaseJob.PRIORITY_ACK_MESSAGE))
            } catch (e: Exception) {
                Timber.e(e)
            }
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

    @Inject
    lateinit var transferServer: TransferServer

    @Inject
    lateinit var transferClient: TransferClient

    @Inject
    lateinit var status: TransferStatusLiveData

    private fun pushRequest() {
        lifecycleScope.launch(SINGLE_SOCKET_THREAD) {
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

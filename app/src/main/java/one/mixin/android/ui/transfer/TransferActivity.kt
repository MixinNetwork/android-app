package one.mixin.android.ui.transfer

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getDeviceId
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
        fun show(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java),
            )
        }
    }

    private val binding by viewBinding(ActivityTransferBinding::inflate)
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
        binding.start.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                transferServer.startServer(false) { transferCommandData ->
                    val qrCode = GsonHelper.customGson.toJson(transferCommandData).generateQRCode(240.dp).first
                    lifecycleScope.launch(Dispatchers.Main) {
                        toast("Sever IP: ${transferCommandData.ip} ${transferCommandData.action}")
                        binding.startClient.isVisible = false
                        binding.start.isVisible = false
                        binding.qr.setImageBitmap(qrCode)
                        binding.qr.fadeIn()
                    }
                }
            }
        }

        binding.startClient.setOnClickListener {
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
                            lifecycleScope.launch(Dispatchers.IO) {
                                TransferClient(finishListener).connectToServer(
                                    it.ip!!,
                                    it.port!!,
                                    TransferCommandData(
                                        this@TransferActivity.getDeviceId(),
                                        TransferCommandAction.CONNECT.value,
                                        1,
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
    }

    override fun onStop() {
        super.onStop()
        disposable?.dispose()
        disposable = null
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

    private fun callbackScan(data: Intent?) {
        val url = data?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT)
        url?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.startClient.isVisible = false
                    binding.start.isVisible = false
                }
                val transferCommandData = GsonHelper.customGson.fromJson(it, TransferCommandData::class.java)
                Timber.e("qrcode:$it")
                TransferClient(finishListener).connectToServer(
                    transferCommandData.ip!!,
                    transferCommandData.port!!,
                    TransferCommandData(
                        this@TransferActivity.getDeviceId(),
                        TransferCommandAction.CONNECT.value,
                        1,
                        code = transferCommandData.code,
                    ),
                )
            }
        }
    }

    private val finishListener: (String) -> Unit = { msg ->
        lifecycleScope.launch(Dispatchers.Main) {
            toast(msg)
            binding.startClient.isVisible = true
            binding.start.isVisible = true
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accountId = Session.getAccountId() ?: return@launch
                val sessionId = Session.getExtensionSessionId() ?: return@launch
                val plainText = gson.toJson(
                    PlainJsonMessagePayload(
                        action = PlainDataAction.DEVICE_TRANSFER.name,
                        content = content,
                    ),
                )
                val encoded = plainText.toByteArray().base64Encode()
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
                this.getDeviceId(),
                TransferCommandAction.PULL.value,
                1,
            ).apply {
                Timber.e("pull ${gson.toJson(this)}")
            },
        )
        sendMessage(encodeText)
    }

    private val transferServer: TransferServer by lazy {
        TransferServer(finishListener)
    }

    private fun pushRequest() {
        lifecycleScope.launch(Dispatchers.IO) {
            transferServer.startServer(true) { transferData ->
                Timber.e("push ${gson.toJson(transferData)}")
                val encodeText = gson.toJson(
                    transferData,
                )
                sendMessage(encodeText)
            }
        }
    }

}

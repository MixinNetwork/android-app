package one.mixin.android.ui.transfer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.util.NetworkUtils
import one.mixin.android.util.viewBinding

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
                withContext(Dispatchers.Main) {
                    toast("Sever IP: ${NetworkUtils.getWifiIpAddress(this@TransferActivity)}")
                    binding.startClient.isVisible = false
                    binding.start.isVisible = false
                }
                val ip = TransferServer(finishListener).startServer()?.generateQRCode(240.dp)
                    ?: return@launch
                withContext(Dispatchers.Main) {
                    binding.qr.setImageBitmap(ip.first)
                    binding.qr.fadeIn()
                }
            }
        }

        binding.startClient.setOnClickListener {
            handleClick()
        }
    }

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>
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
                TransferClient(finishListener).connectToServer(it)
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
}

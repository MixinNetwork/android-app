package one.mixin.android.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
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
        setContentView(binding.root)

        binding.start.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    toast("Sever IP: ${NetworkUtils.getWifiIpAddress(this@TransferActivity)}")
                    binding.startClient.isVisible = false
                    binding.start.isVisible = false
                }
                TransferServer(finishListener).startServer()
            }
        }

        binding.startClient.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.startClient.isVisible = false
                    binding.start.isVisible = false
                }
                TransferClient(finishListener).connectToServer("192.168.98.29")
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

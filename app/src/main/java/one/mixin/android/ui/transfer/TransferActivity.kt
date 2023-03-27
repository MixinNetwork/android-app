package one.mixin.android.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.databinding.ActivityTransferBinding
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.util.NetworkUtils
import one.mixin.android.util.viewBinding

class TransferActivity : BaseActivity() {
    companion object {
        fun show(context: Context) {
            context.startActivity(
                Intent(context, TransferActivity::class.java)
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
                    binding.info.text = "server ${NetworkUtils.getWifiIpAddress(this@TransferActivity)}"
                }
                TransferServer().startServer()
            }
        }

        binding.startClient.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.info.text = NetworkUtils.getWifiIpAddress(this@TransferActivity)
                }
                TransferClient().connectToServer("192.168.12.29")
            }
        }
    }
}
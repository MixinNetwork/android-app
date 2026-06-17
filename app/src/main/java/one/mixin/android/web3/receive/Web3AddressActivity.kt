package one.mixin.android.web3.receive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class Web3AddressActivity : BaseActivity() {
    companion object {
        private const val EXTRA_WEB3_TOKEN = "extra_web3_token"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_HIDE_NETWORK_SWITCH = "extra_hide_network_switch"

        fun show(context: Context, web3Token: Web3TokenItem, address: String, hideNetworkSwitch: Boolean = false) {
            context.startActivity(
                Intent(context, Web3AddressActivity::class.java).apply {
                    putExtra(EXTRA_WEB3_TOKEN, web3Token)
                    putExtra(EXTRA_ADDRESS, address)
                    putExtra(EXTRA_HIDE_NETWORK_SWITCH, hideNetworkSwitch)
                    setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val web3Token = intent.getParcelableExtraCompat(EXTRA_WEB3_TOKEN, Web3TokenItem::class.java)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val hideNetworkSwitch = intent.getBooleanExtra(EXTRA_HIDE_NETWORK_SWITCH, false)

        if (web3Token != null) {
            val fragment = Web3AddressFragment.newInstance(web3Token, address, hideNetworkSwitch)
            replaceFragment(fragment, R.id.container, Web3AddressFragment.TAG)
        } else {
            finish()
        }
    }
}

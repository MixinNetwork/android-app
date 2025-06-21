package one.mixin.android.web3.receive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.web3.receive.Web3AddressFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class Web3AddressActivity : BaseActivity() {
    companion object {
        fun show(context: Context, address: String) {
            context.startActivity(
                Intent(context, Web3AddressActivity::class.java).apply {
                    putExtra(EXTRA_ADDRESS, address)
                    setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
        private const val EXTRA_ADDRESS = "extra_address"
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        val fragment = Web3AddressFragment.newInstance(address)
        replaceFragment(fragment, R.id.container, Web3AddressFragment.TAG)
    }
}


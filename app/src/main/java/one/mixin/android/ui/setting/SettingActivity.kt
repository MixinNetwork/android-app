package one.mixin.android.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class SettingActivity : BlazeBaseActivity() {
    companion object {
        const val FROM_NOTIFICATION = "notification"
        fun show(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    private lateinit var binding: ActivityContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fragment = SettingFragment.newInstance()
        replaceFragment(fragment, R.id.container, SettingFragment.TAG)
        if (intent.getBooleanExtra(FROM_NOTIFICATION, false)) {
            addFragment(fragment, BackUpFragment.newInstance(), BackUpFragment.TAG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pinSettingFragment =
            supportFragmentManager.findFragmentByTag(PinSettingFragment.TAG) as? PinSettingFragment ?: return
        pinSettingFragment.onActivityResult(requestCode, resultCode, data)
    }
}

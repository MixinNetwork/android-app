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
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SettingActivity : BlazeBaseActivity() {
    companion object {
        const val FROM_NOTIFICATION = "notification"
        const val EXTRA_SHOW_PIN_SETTING = "extra_show_pin_setting"
        const val EXTRA_EMERGENCY_CONTACT = "extra_emergency_contact"
        fun show(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }

        fun showPinSetting(context: Context) {
            context.startActivity(
                Intent(context, SettingActivity::class.java).apply {
                    putExtra(EXTRA_SHOW_PIN_SETTING, true)
                }
            )
        }

        fun showEmergencyContact(context: Context) {
            context.startActivity(
                Intent(context, SettingActivity::class.java).apply {
                    putExtra(EXTRA_EMERGENCY_CONTACT, true)
                }
            )
        }
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (intent.getBooleanExtra(EXTRA_SHOW_PIN_SETTING, false)) {
            replaceFragment(PinSettingFragment.newInstance(), R.id.container, PinSettingFragment.TAG)
        } else if (intent.getBooleanExtra(EXTRA_EMERGENCY_CONTACT, false)) {
            replaceFragment(EmergencyContactFragment.newInstance(), R.id.container, EmergencyContactFragment.TAG)
        } else {
            val fragment = SettingFragment.newInstance()
            replaceFragment(fragment, R.id.container, SettingFragment.TAG)
            if (intent.getBooleanExtra(FROM_NOTIFICATION, false)) {
                addFragment(fragment, BackUpFragment.newInstance(), BackUpFragment.TAG)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pinSettingFragment =
            supportFragmentManager.findFragmentByTag(PinSettingFragment.TAG) as? PinSettingFragment ?: return
        pinSettingFragment.onActivityResult(requestCode, resultCode, data)
    }
}

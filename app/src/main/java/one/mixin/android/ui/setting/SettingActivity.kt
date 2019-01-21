package one.mixin.android.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

class SettingActivity : BlazeBaseActivity() {
    companion object {
        const val FROM_NOTIFICATION = "notification"
        fun show(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val fragment = SettingFragment.newInstance()
        replaceFragment(fragment, R.id.container, SettingFragment.TAG)
        if (intent.getBooleanExtra(FROM_NOTIFICATION, false)) {
            addFragment(fragment, BackUpFragment.newInstance(), BackUpFragment.TAG)
        }
    }
}
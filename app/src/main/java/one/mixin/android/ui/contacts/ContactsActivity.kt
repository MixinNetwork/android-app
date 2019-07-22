package one.mixin.android.ui.contacts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

class ContactsActivity : BlazeBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val showProfile = intent?.extras?.getBoolean(ARGS_SHOW_PROFILE) ?: false
        if (showProfile) {
            replaceFragment(ProfileFragment.newInstance(), R.id.container, ProfileFragment.TAG)
        } else {
            replaceFragment(ContactsFragment.newInstance(), R.id.container, ContactsFragment.TAG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val contactsFragment = supportFragmentManager.findFragmentByTag(ContactsFragment.TAG)
        contactsFragment?.onActivityResult(requestCode, resultCode, data)
        val profileFragment = supportFragmentManager.findFragmentByTag(ProfileFragment.TAG)
        profileFragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        const val ARGS_SHOW_PROFILE = "args_show_profile"

        fun show(activity: Activity, showProfile: Boolean = false) {
            val intent = Intent(activity, ContactsActivity::class.java).apply {
                putExtra(ARGS_SHOW_PROFILE, showProfile)
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}

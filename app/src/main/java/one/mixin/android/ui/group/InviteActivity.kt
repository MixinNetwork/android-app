package one.mixin.android.ui.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class InviteActivity : BlazeBaseActivity() {

    companion object {
        var ARGS_ID = "args_id"

        fun show(context: Context, conversationId: String) {
            val intent = Intent(context, InviteActivity::class.java).apply {
                putExtras(InviteFragment.putBundle(conversationId))
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        replaceFragment(InviteFragment.newInstance(intent.extras!!), R.id.container, InviteFragment.TAG)
    }
}

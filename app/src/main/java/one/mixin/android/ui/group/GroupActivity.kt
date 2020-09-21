package one.mixin.android.ui.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_CONVERSATION_ID
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class GroupActivity : BlazeBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val type = intent.getIntExtra(ARGS_TYPE, 0)
        if (type == CREATE) {
            val fragment = GroupFragment.newInstance()
            replaceFragment(fragment, R.id.container, GroupFragment.TAG)
        } else if (type == INFO) {
            val conversationId = intent.getStringExtra(ARGS_CONVERSATION_ID)
            require(conversationId != null)
            val f = GroupInfoFragment.newInstance(conversationId)
            replaceFragment(f, R.id.container, GroupInfoFragment.TAG)
        }
    }

    companion object {
        private const val ARGS_TYPE = "args_type"
        const val ARGS_EXPAND = "args_expand"
        const val CREATE = 0
        const val INFO = 1

        fun show(context: Context, type: Int = CREATE, conversationId: String? = null, expand: Boolean = false) {
            context.startActivity(
                Intent(context, GroupActivity::class.java).apply {
                    putExtra(ARGS_TYPE, type)
                    putExtra(ARGS_EXPAND, expand)
                    conversationId?.let {
                        putExtra(ARGS_CONVERSATION_ID, it)
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val newGroupFragment = supportFragmentManager.findFragmentByTag(NewGroupFragment.TAG)
        newGroupFragment?.onActivityResult(requestCode, resultCode, data)
    }
}

package one.mixin.android.ui.forward

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.Session
import one.mixin.android.util.ShareHelper
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

class ForwardActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_MESSAGES = "args_messages"
        const val ARGS_SHARE = "args_share"
        const val ARGS_FROM_CONVERSATION = "args_from_conversation"

        fun show(
            context: Context,
            messages: ArrayList<ForwardMessage>,
            isShare: Boolean = false,
            fromConversation: Boolean = false
        ) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_MESSAGES, messages)
                putExtra(ARGS_SHARE, isShare)
                putExtra(ARGS_FROM_CONVERSATION, fromConversation)
            }
            intent.flags = FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun show(context: Context, link: String?) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                val list = ArrayList<ForwardMessage>().apply {
                    add(ForwardMessage(ForwardCategory.TEXT.name, content = link))
                }
                putParcelableArrayListExtra(ARGS_MESSAGES, list)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val list = intent.getParcelableArrayListExtra<ForwardMessage>(ARGS_MESSAGES)
        if (list != null && list.isNotEmpty()) {
            val f = ForwardFragment.newInstance(
                list,
                intent.getBooleanExtra(ARGS_SHARE, false),
                intent.getBooleanExtra(ARGS_FROM_CONVERSATION, false)
            )
            replaceFragment(f, R.id.container, ForwardFragment.TAG)
        } else {
            if (Session.getAccount() == null) {
                toast(R.string.not_logged_in)
                finish()
                return
            }
            val forwardMessageList = ShareHelper.get().generateForwardMessageList(intent)
            if (forwardMessageList != null && forwardMessageList.isNotEmpty()) {
                replaceFragment(ForwardFragment.newInstance(forwardMessageList, true), R.id.container, ForwardFragment.TAG)
            } else {
                toast(R.string.error_share)
                finish()
            }
        }
    }
}

package one.mixin.android.ui.forward

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment.Companion.CATEGORY
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment.Companion.CONTENT
import one.mixin.android.util.Session
import one.mixin.android.util.ShareHelper
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage

@AndroidEntryPoint
class ForwardActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_MESSAGES = "args_messages"
        const val ARGS_SHARE = "args_share"
        const val ARGS_SELECT = "args_select"
        const val ARGS_FROM_CONVERSATION = "args_from_conversation"
        const val ARGS_RESULT = "args_result"

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

        fun send(context: Context, category: String, content: String) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                putExtra(ARGS_SELECT, true)
                putExtra(CATEGORY, category)
                putExtra(CONTENT, content)
            }
            context.startActivity(intent)
        }
    }

    class ForwardContract : ActivityResultContract<Intent?, Intent?>() {
        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }

        override fun createIntent(context: Context, input: Intent?): Intent {
            return Intent(context, ForwardActivity::class.java).apply {
                putExtra(ARGS_SELECT, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val list = intent.getParcelableArrayListExtra<ForwardMessage>(ARGS_MESSAGES)
        if (intent.getBooleanExtra(ARGS_SELECT, false)) {
            val f = ForwardFragment.newInstance(
                content = intent.getStringExtra(CONTENT),
                category = intent.getStringExtra(CATEGORY)
            )
            replaceFragment(f, R.id.container, ForwardFragment.TAG)
        } else if (list != null && list.isNotEmpty()) {
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

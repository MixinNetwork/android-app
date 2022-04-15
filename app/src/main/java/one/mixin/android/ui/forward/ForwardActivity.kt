package one.mixin.android.ui.forward

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.util.ShareHelper
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.TranscriptMessage

@AndroidEntryPoint
class ForwardActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_MESSAGES = "args_messages"
        const val ARGS_COMBINE_MESSAGES = "args_combine_messages"
        const val ARGS_ACTION = "args_action"

        const val ARGS_RESULT = "args_result"

        inline fun show(
            context: Context,
            messages: ArrayList<ForwardMessage>,
            action: ForwardAction
        ) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_MESSAGES, messages)
                putExtra(ARGS_ACTION, action)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun show(context: Context, link: String) {
            val list = ArrayList<ForwardMessage>().apply {
                add(ForwardMessage(ShareCategory.Text, content = link))
            }
            show(context, list, ForwardAction.App.Resultless())
        }

        fun combineForward(context: Context, messages: ArrayList<TranscriptMessage>) {
            val intent = Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_COMBINE_MESSAGES, messages)
                putExtra(ARGS_ACTION, ForwardAction.Combine())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    class ForwardContract : ActivityResultContract<Pair<ArrayList<ForwardMessage>, String?>, Intent?>() {
        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }

        override fun createIntent(context: Context, input: Pair<ArrayList<ForwardMessage>, String?>): Intent {
            return Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_MESSAGES, input.first)
                putExtra(ARGS_ACTION, ForwardAction.App.Resultful(input.second))
            }
        }
    }

    class CombineForwardContract : ActivityResultContract<ArrayList<TranscriptMessage>, Intent?>() {
        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }

        override fun createIntent(context: Context, input: ArrayList<TranscriptMessage>): Intent {
            return Intent(context, ForwardActivity::class.java).apply {
                putParcelableArrayListExtra(ARGS_COMBINE_MESSAGES, input)
                putExtra(ARGS_ACTION, ForwardAction.Combine())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val list = intent.getParcelableArrayListExtra<ForwardMessage>(ARGS_MESSAGES)
        val action = intent.getParcelableExtra<ForwardAction>(ARGS_ACTION)
        if (action != null && list != null && list.isNotEmpty()) {
            val f = ForwardFragment.newInstance(list, action)
            replaceFragment(f, R.id.container, ForwardFragment.TAG)
        } else if (action is ForwardAction.Combine) {
            val f = ForwardFragment.newCombineInstance(
                requireNotNull(
                    intent.getParcelableArrayListExtra(
                        ARGS_COMBINE_MESSAGES
                    )
                ),
                action
            )
            replaceFragment(f, R.id.container, ForwardFragment.TAG)
        } else {
            if (Session.getAccount() == null) {
                toast(R.string.not_logged_in)
                finish()
                return
            }
            val forwardMessageList = ShareHelper.get().generateForwardMessageList(intent)
            val conversationId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && intent.hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
                intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
            } else null
            if (!forwardMessageList.isNullOrEmpty()) {
                replaceFragment(
                    ForwardFragment.newInstance(
                        forwardMessageList,
                        ForwardAction.System(conversationId, getString(R.string.Share))
                    ),
                    R.id.container, ForwardFragment.TAG
                )
            } else {
                toast(R.string.error_share)
                finish()
            }
        }
    }
}

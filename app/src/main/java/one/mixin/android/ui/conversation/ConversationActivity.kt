package one.mixin.android.ui.conversation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT_ID
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.TranscriptData
import one.mixin.android.vo.User
import javax.inject.Inject

@AndroidEntryPoint
class ConversationActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(ARGS_FAST_SHOW, false)) {
                replaceFragment(
                    ConversationFragment.newInstance(intent.extras!!),
                    R.id.container,
                    ConversationFragment.TAG,
                )
            } else {
                showConversation(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showConversation(intent)
    }

    override fun finish() {
        if (intent.getBooleanExtra(ARGS_SHORTCUT, false)) {
            MainActivity.show(this)
        }
        super.finish()
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var userRepository: UserRepository

    private fun showConversation(intent: Intent) {
        val bundle = intent.extras ?: return
        lifecycleScope.launch(
            CoroutineExceptionHandler { _, _ ->
                replaceFragment(
                    ConversationFragment.newInstance(intent.extras!!),
                    R.id.container,
                    ConversationFragment.TAG,
                )
            },
        ) {
            val conversationId = bundle.getString(CONVERSATION_ID)
            val userId = bundle.getString(RECIPIENT_ID)
            if (userId != null) {
                val user = userRepository.suspendFindUserById(userId)
                require(user != null && userId != Session.getAccountId()) {
                    "error data userId: $userId"
                }
                bundle.putParcelable(RECIPIENT, user)
            } else {
                val user = userRepository.suspendFindContactByConversationId(conversationId!!)
                require(user?.userId != Session.getAccountId()) {
                    "error data conversationId: $conversationId"
                }
                bundle.putParcelable(RECIPIENT, user)
            }
            replaceFragment(
                ConversationFragment.newInstance(bundle),
                R.id.container,
                ConversationFragment.TAG,
            )
        }
    }

    companion object {
        private const val ARGS_FAST_SHOW = "args_fast_show"
        const val ARGS_SHORTCUT = "args_shortcut"

        fun fastShow(
            context: Context,
            conversationId: String,
            recipient: User?,
        ) {
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    Bundle().apply {
                        putString(CONVERSATION_ID, conversationId)
                        putParcelable(RECIPIENT, recipient)
                        putBoolean(ARGS_FAST_SHOW, true)
                    },
                )
            }.run {
                context.startActivity(this)
            }
        }

        fun getShortcutIntent(
            context: Context,
            conversationId: String,
            recipientId: String? = null,
        ): Intent {
            return putIntent(context, conversationId, recipientId = recipientId).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_CLEAR_TASK)
                addCategory(Intent.CATEGORY_LAUNCHER)
                putExtra(ARGS_SHORTCUT, true)
                action = Intent.ACTION_VIEW
            }
        }

        fun show(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
            messageId: String? = null,
            keyword: String? = null,
            transcriptData: TranscriptData? = null,
            startParam: String? = null,
        ) {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        keyword,
                        messageId,
                        transcriptData,
                        startParam,
                    ),
                )
                if (context !is Activity){
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }.run {
                context.startActivity(this)
            }
        }

        fun putIntent(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
            messageId: String? = null,
            keyword: String? = null,
        ): Intent {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            return Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        keyword,
                        messageId,
                    ),
                )
            }
        }

        fun showAndClear(
            context: Context,
            conversationId: String?,
            messageId: String? = null,
            recipientId: String? = null,
            keyword: String? = null,
        ) {
            val mainIntent = Intent(context, ConversationActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val conversationIntent = putIntent(context, conversationId, recipientId, messageId, keyword)
            context.startActivities(arrayOf(mainIntent, conversationIntent))
        }
    }
}

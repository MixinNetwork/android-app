package one.mixin.android.ui.conversation

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.os.Build
import android.os.Bundle
import android.view.Display
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
import one.mixin.android.ui.conversation.ConversationFragment.Companion.INITIAL_POSITION_MESSAGE_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.MESSAGE_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.UNREAD_COUNT
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.vo.TranscriptData
import one.mixin.android.vo.User
import one.mixin.android.vo.generateConversationId
import javax.inject.Inject

@AndroidEntryPoint
class ConversationActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isBubbled = if (Build.VERSION.SDK_INT >= 31) {
            isLaunchedFromBubble
        } else {
            val displayId = if (Build.VERSION.SDK_INT >= 30) {
                display?.displayId
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.displayId
            }
            displayId != Display.DEFAULT_DISPLAY
        }
        setContentView(R.layout.activity_chat)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (intent.getBooleanExtra(ARGS_FAST_SHOW, false)) {
            replaceFragment(
                ConversationFragment.newInstance(intent.extras!!),
                R.id.container,
                ConversationFragment.TAG
            )
        } else {
            showConversation(intent)
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

    var isBubbled = false

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
                    ConversationFragment.TAG
                )
            }
        ) {
            val messageId = bundle.getString(MESSAGE_ID)
            val conversationId = bundle.getString(CONVERSATION_ID)
            val userId = bundle.getString(RECIPIENT_ID)
            var unreadCount = bundle.getInt(UNREAD_COUNT, -1)
            val cid: String
            if (userId != null) {
                val user = userRepository.suspendFindUserById(userId)
                val accountId = Session.getAccountId() ?: return@launch
                cid = conversationId ?: generateConversationId(accountId, userId)
                require(user != null && userId != Session.getAccountId()) {
                    "error data userId: $userId"
                }
                bundle.putParcelable(RECIPIENT, user)
            } else {
                val user = userRepository.suspendFindContactByConversationId(conversationId!!)
                require(user?.userId != Session.getAccountId()) {
                    "error data conversationId: $conversationId"
                }
                cid = conversationId
                bundle.putParcelable(RECIPIENT, user)
            }
            if (unreadCount == -1) {
                unreadCount = if (!messageId.isNullOrEmpty()) {
                    conversationRepository.findMessageIndex(cid, messageId)
                } else {
                    conversationRepository.indexUnread(cid) ?: -1
                }
            }
            bundle.putInt(UNREAD_COUNT, unreadCount)
            val msgId = messageId ?: if (unreadCount <= 0) {
                null
            } else {
                conversationRepository.findFirstUnreadMessageId(cid, unreadCount - 1)
            }
            bundle.putString(INITIAL_POSITION_MESSAGE_ID, msgId)
            replaceFragment(
                ConversationFragment.newInstance(bundle),
                R.id.container,
                ConversationFragment.TAG
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
            initialPositionMessageId: String?,
            unreadCount: Int
        ) {
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    Bundle().apply {
                        putString(CONVERSATION_ID, conversationId)
                        putParcelable(RECIPIENT, recipient)
                        putString(INITIAL_POSITION_MESSAGE_ID, initialPositionMessageId)
                        putInt(UNREAD_COUNT, unreadCount)
                        putBoolean(ARGS_FAST_SHOW, true)
                    }
                )
            }.run {
                context.startActivity(this)
            }
        }

        fun getShortcutIntent(context: Context, conversationId: String, recipientId: String? = null): Intent {
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
            unreadCount: Int? = null,
            transcriptData: TranscriptData? = null
        ) {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        messageId,
                        keyword,
                        unreadCount,
                        transcriptData
                    )
                )
            }.run {
                context.startActivity(this)
            }
        }

        fun putIntent(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
            messageId: String? = null,
            keyword: String? = null
        ): Intent {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            return Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        messageId,
                        keyword,
                    )
                )
            }
        }

        fun showAndClear(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
            messageId: String? = null,
            keyword: String? = null
        ) {
            val mainIntent = Intent(context, ConversationActivity::class.java)
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val conversationIntent = putIntent(context, conversationId, recipientId, messageId, keyword)
            context.startActivities(arrayOf(mainIntent, conversationIntent))
        }
    }
}

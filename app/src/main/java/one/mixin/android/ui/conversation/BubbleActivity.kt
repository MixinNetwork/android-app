package one.mixin.android.ui.conversation

import android.content.Context
import android.content.Intent
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
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT_ID
import javax.inject.Inject

@AndroidEntryPoint
class BubbleActivity : BlazeBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isBubbled =
            if (Build.VERSION.SDK_INT >= 31) {
                isLaunchedFromBubble
            } else {
                val displayId =
                    if (Build.VERSION.SDK_INT >= 30) {
                        display?.displayId
                    } else {
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.displayId
                    }
                displayId != Display.DEFAULT_DISPLAY
            }
        setContentView(R.layout.activity_chat)
        showConversation(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showConversation(intent)
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
        fun putIntent(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
        ): Intent {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            return Intent(context, BubbleActivity::class.java).apply {
                putExtras(ConversationFragment.putBundle(conversationId, recipientId, null, null))
            }
        }
    }
}

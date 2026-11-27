package one.mixin.android.ui.conversation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.os.Bundle
import android.view.KeyEvent
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
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.vo.TranscriptData
import one.mixin.android.vo.User
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Job
import timber.log.Timber

@AndroidEntryPoint
class ConversationActivity : BlazeBaseActivity() {
    private var suppressHomeFallbackOnFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val hasAccount = Session.getAccount() != null
        super.onCreate(if (hasAccount) savedInstanceState else null)
        if (!hasAccount) {
            suppressHomeFallbackOnFinish = true
            LandingActivity.show(this)
            finish()
            return
        }
        if (ConversationEmbedding.routeRootConversation(this) { suppressHomeFallbackOnFinish = true }) return
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
        if (Session.getAccount() == null) {
            suppressHomeFallbackOnFinish = true
            LandingActivity.show(this)
            finish()
            return
        }
        setIntent(intent)
        showConversation(intent)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && event != null && !event.isCanceled && event.hasNoModifiers()) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onBackPressedAtRoot(): Boolean {
        if (!shouldReturnToMainOnExit(isTaskRoot, Session.checkToken(), suppressHomeFallbackOnFinish)) {
            return false
        }
        finish()
        return true
    }

    override fun finish() {
        if (shouldReturnToMainOnExit(isTaskRoot, Session.checkToken(), suppressHomeFallbackOnFinish)) {
            suppressHomeFallbackOnFinish = true
            MainActivity.show(this)
        }
        super.finish()
    }

    @Inject
    lateinit var conversationRepositoryProvider: Provider<ConversationRepository>

    @Inject
    lateinit var userRepositoryProvider: Provider<UserRepository>

    private val userRepository: UserRepository
        get() = userRepositoryProvider.get()

    private var conversationJob: Job? = null

    private fun showConversation(intent: Intent) {
        val bundle = intent.extras ?: return
        conversationJob?.cancel()
        conversationJob = lifecycleScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                if (Session.getAccount() == null) {
                    suppressHomeFallbackOnFinish = true
                    LandingActivity.show(this)
                    finish()
                    return@CoroutineExceptionHandler
                }
                val fallbackExtras = intent.extras
                if (fallbackExtras == null || (fallbackExtras.getString(CONVERSATION_ID) == null && fallbackExtras.getString(RECIPIENT_ID) == null)) {
                    Timber.e(throwable, "Invalid conversation intent extras")
                    finish()
                    return@CoroutineExceptionHandler
                }
                replaceFragment(
                    ConversationFragment.newInstance(fallbackExtras),
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

        internal fun shouldReturnToMainOnExit(
            isTaskRoot: Boolean,
            hasSession: Boolean,
            suppressHomeFallback: Boolean,
        ): Boolean = !suppressHomeFallback && hasSession && isTaskRoot

        fun fastShow(
            context: Context,
            conversationId: String,
            recipient: User?,
            initialUnreadMessageId: String? = null,
            initialUnreadCount: Int? = null,
        ) {
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(
                    Bundle().apply {
                        putString(CONVERSATION_ID, conversationId)
                        putParcelable(RECIPIENT, recipient)
                        putString(ConversationFragment.INITIAL_UNREAD_MESSAGE_ID, initialUnreadMessageId)
                        initialUnreadCount?.let {
                            putInt(ConversationFragment.INITIAL_UNREAD_COUNT, it)
                        }
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
            initialUnreadCount: Int? = null,
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
                        initialUnreadCount,
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
            initialUnreadCount: Int? = null,
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
                        initialUnreadCount = initialUnreadCount,
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
            val conversationIntent = putIntent(context, conversationId, recipientId, messageId, keyword)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(conversationIntent)
        }
    }
}

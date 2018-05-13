package one.mixin.android.ui.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.User
import javax.inject.Inject

class ConversationActivity : BlazeBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        container.backgroundImage = resources.getDrawable(R.drawable.bg_chat, null)
        showConversation(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showConversation(intent)
    }

    @Inject
    lateinit var conversationRepository: ConversationRepository

    private fun showConversation(intent: Intent) {
        val bundle = intent.extras
        if (bundle.getString(CONVERSATION_ID) == null) {
            val user = bundle.getParcelable<User>(RECIPIENT)
            Observable.just(user).map {
                conversationRepository.getConversationIdIfExistsSync(user.userId)
            }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(scopeProvider)
                .subscribe({
                    bundle.putString(CONVERSATION_ID, it)
                    replaceFragment(ConversationFragment.newInstance(bundle), R.id.container)
                }, {
                    replaceFragment(ConversationFragment.newInstance(intent.extras), R.id.container)
                })
        } else {
            replaceFragment(ConversationFragment.newInstance(intent.extras), R.id.container)
        }
    }

    companion object {
        fun show(
            context: Context,
            conversationId: String? = null,
            recipient: User? = null,
            isGroup: Boolean = false,
            messageId: String? = null,
            keyword: String? = null,
            messages: ArrayList<ForwardMessage>? = null,
            isBot: Boolean = false
        ) {
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(ConversationFragment.putBundle(conversationId,
                    recipient, isGroup, messageId, keyword, messages, isBot))
            }.run {
                context.startActivity(this)
            }
        }

        fun putIntent(
            context: Context,
            conversationId: String? = null,
            recipient: User? = null,
            isGroup: Boolean = false,
            messageId: String? = null,
            keyword: String? = null,
            messages: ArrayList<ForwardMessage>? = null
        ) =
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(ConversationFragment.putBundle(conversationId,
                    recipient, isGroup, messageId, keyword, messages))
            }
    }
}
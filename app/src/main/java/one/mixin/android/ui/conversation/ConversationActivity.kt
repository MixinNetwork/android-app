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
import one.mixin.android.repository.QrCodeType
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT_ID
import one.mixin.android.vo.ForwardMessage
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
    @Inject
    lateinit var userRepository: UserRepository

    private fun showConversation(intent: Intent) {
        val bundle = intent.extras ?: return
        if (bundle.getString(CONVERSATION_ID) == null) {
            val userId = bundle.getString(RECIPIENT_ID)
            Observable.just(QrCodeType.user).map {
                userRepository.getUserById(userId)!!
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).autoDisposable(scopeProvider)
                .subscribe({
                    bundle.putParcelable(RECIPIENT, it)
                    replaceFragment(ConversationFragment.newInstance(bundle), R.id.container)
                }, {
                    replaceFragment(ConversationFragment.newInstance(intent.extras), R.id.container)
                })
        } else {
            Observable.just(bundle.getString(CONVERSATION_ID)).map {
                userRepository.findContactByConversationId(it)
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).autoDisposable(scopeProvider)
                .subscribe({
                    bundle.putParcelable(RECIPIENT, it)
                    replaceFragment(ConversationFragment.newInstance(bundle), R.id.container)
                }, {
                    replaceFragment(ConversationFragment.newInstance(intent.extras), R.id.container)
                })
        }
    }

    companion object {

        fun show(
            context: Context,
            conversationId: String? = null,
            recipientId: String? = null,
            messageId: String? = null,
            keyword: String? = null,
            messages: ArrayList<ForwardMessage>? = null
        ) {
            if (conversationId == null && recipientId == null) {
                throw IllegalArgumentException("lose data")
            }
            Intent(context, ConversationActivity::class.java).apply {
                putExtras(ConversationFragment.putBundle(conversationId, recipientId, messageId, keyword, messages))
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
            messages: ArrayList<ForwardMessage>? = null
        ): Intent {
            if (conversationId == null && recipientId == null) {
                throw IllegalArgumentException("lose data")
            }
            return Intent(context, ConversationActivity::class.java).apply {
                putExtras(ConversationFragment.putBundle(conversationId, recipientId, messageId, keyword, messages))
            }
        }
    }
}
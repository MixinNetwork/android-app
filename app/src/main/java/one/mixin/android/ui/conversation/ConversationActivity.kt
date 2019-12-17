package one.mixin.android.ui.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import com.uber.autodispose.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_chat.*
import one.mixin.android.R
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.replaceFragment
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT
import one.mixin.android.ui.conversation.ConversationFragment.Companion.RECIPIENT_ID
import one.mixin.android.util.Session
import one.mixin.android.vo.ForwardMessage

class ConversationActivity : BlazeBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        if (booleanFromAttribute(R.attr.flag_night)) {
            container.backgroundImage = resources.getDrawable(R.drawable.bg_chat_night, theme)
        } else {
            container.backgroundImage = resources.getDrawable(R.drawable.bg_chat, theme)
        }
        showConversation(intent)
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LAYOUT_STABLE
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
            val userId = bundle.getString(RECIPIENT_ID)!!
            Observable.just(userId).map {
                userRepository.getUserById(userId)!!
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe({
                    require(it.userId != Session.getAccountId()) { "error data" }
                    bundle.putParcelable(RECIPIENT, it)
                    replaceFragment(
                        ConversationFragment.newInstance(bundle),
                        R.id.container,
                        ConversationFragment.TAG
                    )
                }, {
                    replaceFragment(
                        ConversationFragment.newInstance(intent.extras!!),
                        R.id.container,
                        ConversationFragment.TAG
                    )
                })
        } else {
            Observable.just(bundle.getString(CONVERSATION_ID)).map {
                userRepository.findContactByConversationId(it)
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .autoDispose(stopScope)
                .subscribe({
                    if (it?.userId == Session.getAccountId()) {
                        throw IllegalArgumentException(
                            "error data ${bundle.getString(
                                CONVERSATION_ID
                            )}"
                        )
                    }
                    bundle.putParcelable(RECIPIENT, it)
                    replaceFragment(
                        ConversationFragment.newInstance(bundle),
                        R.id.container,
                        ConversationFragment.TAG
                    )
                }, {
                    replaceFragment(
                        ConversationFragment.newInstance(intent.extras!!),
                        R.id.container,
                        ConversationFragment.TAG
                    )
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
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            Intent(context, ConversationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        messageId,
                        keyword,
                        messages
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
            keyword: String? = null,
            messages: ArrayList<ForwardMessage>? = null
        ): Intent {
            require(!(conversationId == null && recipientId == null)) { "lose data" }
            require(recipientId != Session.getAccountId()) { "error data $conversationId" }
            return Intent(context, ConversationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtras(
                    ConversationFragment.putBundle(
                        conversationId,
                        recipientId,
                        messageId,
                        keyword,
                        messages
                    )
                )
            }
        }
    }
}

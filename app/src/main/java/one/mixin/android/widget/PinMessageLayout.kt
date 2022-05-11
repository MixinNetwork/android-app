package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ViewPinMessageLayoutBinding
import one.mixin.android.event.PinMessageEvent
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.sharedPreferences
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.session.Session
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.PinMessageItem
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.explain

class PinMessageLayout constructor(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
    private val binding =
        ViewPinMessageLayoutBinding.inflate(LayoutInflater.from(context), this)
    private val pinContentTv = binding.pinContentTv
    private val pinContent = binding.pinContent
    private val pinClose = binding.pinClose
    val pin = binding.pin
    var conversationId: String? = null
        set(value) {
            field = value
            if (value != null) {
                pinContent.isInvisible =
                    !context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                        .getBoolean("Pin_$conversationId", true)
            }
        }

    private fun collapse() {
        if (pinContent.isVisible) {
            val cx = pinContent.width
            val cy = pinContent.height / 2
            val initialRadius = pinContent.width.toFloat()
            val anim =
                ViewAnimationUtils.createCircularReveal(pinContent, cx, cy, initialRadius, 0f)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    pinContent.visibility = View.INVISIBLE
                }
            })
            anim.start()
        }
    }

    private var disposable: Disposable? = null
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        disposable = RxBus.listen(PinMessageEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.conversationId == conversationId) {
                    context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                        .putBoolean("Pin_$conversationId", true)
                    expand()
                }
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    private fun expand() {
        if (!pinContent.isVisible) {
            val cx = pinContent.width
            val cy = pinContent.height / 2
            val finalRadius = pinContent.width.toFloat()
            val anim = ViewAnimationUtils.createCircularReveal(pinContent, cx, cy, 0f, finalRadius)
            pinContent.visibility = View.VISIBLE
            anim.start()
        }
    }

    fun bind(message: PinMessageItem, clickAction: (String) -> Unit) {
        val pinMessage = try {
            GsonHelper.customGson.fromJson(message.content, PinMessageMinimal::class.java)
        } catch (e: Exception) {
            null
        }
        pinClose.setOnClickListener {
            if (pinContent.isVisible) {
                collapse()
            }
            context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                .putBoolean("Pin_$conversationId", false)
        }
        pinContent.setOnClickListener {
            pinMessage?.let { msg ->
                clickAction.invoke(msg.messageId)
            }
        }
        if (message.mentions != null) {
            pinContentTv.renderMessage(
                String.format(
                    getText(R.string.chat_pin_message),
                    if (Session.getAccountId() == message.userId) {
                        getText(R.string.You)
                    } else {
                        message.userFullName
                    },
                    pinMessage?.let { msg ->
                        " \"${msg.content}\""
                    } ?: getText(R.string.a_message)
                ),
                MentionRenderCache.singleton.getMentionRenderContext(
                    message.mentions
                )
            )
        } else {
            pinContentTv.text =
                String.format(
                    getText(R.string.chat_pin_message),
                    if (Session.getAccountId() == message.userId) {
                        getText(R.string.You)
                    } else {
                        message.userFullName
                    },
                    pinMessage.explain(binding.root.context)
                )
        }
    }

    private fun getText(@StringRes res: Int): String {
        return context.getString(res)
    }
}

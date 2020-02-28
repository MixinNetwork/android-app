package one.mixin.android.ui.conversation.holder

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.BlinkEvent
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getColorCode
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageStatus

abstract class BaseViewHolder constructor(containerView: View) :
    RecyclerView.ViewHolder(containerView) {
    companion object {
        private val colors: IntArray =
            MixinApplication.appContext.resources.getIntArray(R.array.name_colors)
        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
        val SELECT_COLOR = Color.parseColor("#660D94FC")

        fun getColorById(id: String) = colors[id.getColorCode(CodeType.Name(colors.size))]
    }

    protected val dp3 by lazy {
        MixinApplication.appContext.dpToPx(3f)
    }
    protected val dp8 by lazy {
        MixinApplication.appContext.dpToPx(8f)
    }
    protected val dp10 by lazy {
        MixinApplication.appContext.dpToPx(10f)
    }
    protected val dp12 by lazy {
        MixinApplication.appContext.dpToPx(12f)
    }

    protected var isMe = false

    protected open fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean = false) {
        this.isMe = isMe
    }

    private fun chatLayout(isLast: Boolean) {
        chatLayout(isMe, isLast, true)
    }

    protected val botIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_bot)?.also {
            it.setBounds(0, 0, dp12, dp12)
        }
    }

    private val isNightMode by lazy {
        itemView.context.booleanFromAttribute(R.attr.flag_night)
    }

    protected fun setItemBackgroundResource(view: View, @DrawableRes defaultBg: Int, @DrawableRes nightBg: Int) {
        view.setBackgroundResource(
            if (!isNightMode) {
                defaultBg
            } else {
                nightBg
            }
        )
    }

    val meId by lazy {
        Session.getAccountId()
    }

    private var disposable: Disposable? = null
    protected var messageId: String? = null

    fun listen(bindId: String) {
        if (disposable == null) {
            disposable = RxBus.listen(BlinkEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.messageId == this.messageId) {
                        if (it.type != null) {
                            chatLayout(it.type)
                        } else {
                            blink()
                        }
                    }
                }
        }
        this.messageId = bindId
    }

    fun stopListen() {
        disposable?.dispose()
        disposable = null
    }

    protected fun blink() {
        if (!blinkAnim.isRunning) {
            blinkAnim.start()
        }
    }

    private val argbEvaluator: ArgbEvaluator by lazy {
        ArgbEvaluator()
    }
    private val blinkAnim by lazy {
        ValueAnimator.ofFloat(0f, 1f, 0f)
            .setDuration(1200).apply {
                this.addUpdateListener { valueAnimator ->
                    itemView.setBackgroundColor(
                        argbEvaluator.evaluate(
                            valueAnimator.animatedValue as Float,
                            Color.TRANSPARENT,
                            SELECT_COLOR
                        ) as Int
                    )
                }
            }
    }

    protected fun setStatusIcon(
        isMe: Boolean,
        status: String,
        isSecret: Boolean,
        handleAction: (Drawable?, Drawable?) -> Unit
    ) {
        setStatusIcon(isMe, status, isSecret, false, handleAction)
    }

    protected fun setStatusIcon(
        isMe: Boolean,
        status: String,
        isSecret: Boolean,
        isWhite: Boolean,
        handleAction: (Drawable?, Drawable?) -> Unit
    ) {
        val secretIcon = if (isSecret) {
            if (isWhite) {
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_secret_white)
            } else {
                AppCompatResources.getDrawable(itemView.context, R.drawable.ic_secret)
            }
        } else {
            null
        }
        if (isMe) {
            val statusIcon: Drawable? =
                when (status) {
                    MessageStatus.SENDING.name ->
                        AppCompatResources.getDrawable(
                            itemView.context,
                            if (isWhite) {
                                R.drawable.ic_status_sending_white
                            } else {
                                R.drawable.ic_status_sending
                            }
                        )
                    MessageStatus.SENT.name ->
                        AppCompatResources.getDrawable(
                            itemView.context,
                            if (isWhite) {
                                R.drawable.ic_status_sent_white
                            } else {
                                R.drawable.ic_status_sent
                            }
                        )
                    MessageStatus.DELIVERED.name ->
                        AppCompatResources.getDrawable(
                            itemView.context, if (isWhite) {
                                R.drawable.ic_status_delivered_white
                            } else {
                                R.drawable.ic_status_delivered
                            }
                        )
                    MessageStatus.READ.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_read)
                    else -> null
                }
            handleAction(statusIcon, secretIcon)
        } else {
            handleAction(null, secretIcon)
        }
    }
}

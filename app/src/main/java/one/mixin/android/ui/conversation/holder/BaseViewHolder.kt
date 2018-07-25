package one.mixin.android.ui.conversation.holder

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.BlinkEvent
import one.mixin.android.extension.dpToPx
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageStatus

abstract class BaseViewHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    companion object {
        val colors: IntArray = MixinApplication.appContext.resources.getIntArray(R.array.name_colors)
        val HIGHLIGHTED = Color.parseColor("#CCEF8C")
        val LINK_COLOR = Color.parseColor("#5FA7E4")
        val SELECT_COLOR = Color.parseColor("#660D94FC")
    }

    protected val dp10 by lazy {
        MixinApplication.appContext.dpToPx(10f)
    }
    private val dp12 by lazy {
        MixinApplication.appContext.dpToPx(12f)
    }

    abstract fun chatLayout(isMe: Boolean, isLast: Boolean)

    protected val botIcon: Drawable? by lazy {
        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_bot)?.also {
            it.setBounds(0, 0, dp12, dp12)
        }
    }

    val meId by lazy {
        Session.getAccountId()
    }

    private var disposable: Disposable? = null
    private var messageId: String? = null

    fun listen(bindId: String) {
        if (disposable == null) {
            disposable = RxBus.listen(BlinkEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.messageId == this.messageId) {
                        blink()
                    }
                }
        }
        this.messageId = bindId
    }

    fun stopListen() {
        disposable?.dispose()
        disposable = null
    }

    private fun blink() {
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
                        argbEvaluator.evaluate(valueAnimator.animatedValue as Float, Color.TRANSPARENT, SELECT_COLOR) as Int)
                }
            }
    }

    protected fun setStatusIcon(
        isMe: Boolean,
        status: String,
        setIcon: (Drawable?) -> Unit,
        hideIcon: () -> Unit,
        isWhite: Boolean = false
    ) {
        if (isMe) {
            val drawable: Drawable? =
                when (status) {
                    MessageStatus.SENDING.name ->
                        AppCompatResources.getDrawable(itemView.context,
                            if (isWhite) {
                                R.drawable.ic_status_sending_white
                            } else {
                                R.drawable.ic_status_sending
                            })
                    MessageStatus.SENT.name ->
                        AppCompatResources.getDrawable(itemView.context,
                            if (isWhite) {
                                R.drawable.ic_status_sent_white
                            } else {
                                R.drawable.ic_status_sent
                            })
                    MessageStatus.DELIVERED.name ->
                        AppCompatResources.getDrawable(itemView.context, if (isWhite) {
                            R.drawable.ic_status_delivered_white
                        } else {
                            R.drawable.ic_status_delivered
                        })
                    MessageStatus.READ.name ->
                        AppCompatResources.getDrawable(itemView.context, R.drawable.ic_status_read)
                    else -> null
                }
            drawable.also {
                it?.setBounds(0, 0, dp10, dp10)
            }
            setIcon(drawable)
        } else {
            hideIcon()
        }
    }
}
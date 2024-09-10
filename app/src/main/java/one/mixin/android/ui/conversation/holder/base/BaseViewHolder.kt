package one.mixin.android.ui.conversation.holder.base

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TooltipCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.BlinkEvent
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fullTime
import one.mixin.android.extension.getColorCode
import one.mixin.android.session.Session
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.membershipIcon

abstract class BaseViewHolder constructor(containerView: View) :
    RecyclerView.ViewHolder(containerView) {
        companion object {
            private val colors: IntArray = MixinApplication.appContext.resources.getIntArray(R.array.name_colors)

            @ColorInt
            fun getColorById(id: String) = colors[id.getColorCode(CodeType.Name(colors.size))]
        }

        protected val dp1 by lazy {
            MixinApplication.appContext.dpToPx(1f)
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
        var canNotReply = true

        protected open fun bind(messageItem: MessageItem) {
            canNotReply = messageItem.canNotReply()
            if (this is Terminable && itemView.isShown) {
                onRead(messageItem.messageId, messageItem.expireIn, messageItem.expireAt)
            }
        }

        protected open fun chatLayout(
            isMe: Boolean,
            isLast: Boolean,
            isBlink: Boolean = false,
        ) {
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

        protected fun getMembershipBadge(messageItem: MessageItem): Drawable? {
            return messageItem.membership.membershipIcon()?.let { icon ->
                if (icon == View.NO_ID) {
                    null
                } else {
                    AppCompatResources.getDrawable(itemView.context, icon)?.also {
                        it.setBounds(0, 0, dp12, dp12)
                    }
                }
            }
        }

        protected val isNightMode by lazy {
            itemView.context.booleanFromAttribute(R.attr.flag_night)
        }

        protected fun setItemBackgroundResource(
            view: View,
            @DrawableRes defaultBg: Int,
            @DrawableRes nightBg: Int,
        ) {
            view.setBackgroundResource(
                if (!isNightMode) {
                    defaultBg
                } else {
                    nightBg
                },
            )
        }

        val meId by lazy {
            Session.getAccountId()
        }

        private var disposable: Disposable? = null
        protected var messageId: String? = null

        fun listen(
            view: View,
            bindId: String,
        ) {
            if (disposable == null) {
                disposable =
                    RxBus.listen(BlinkEvent::class.java)
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDispose(view)
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

        fun chatJumpLayout(
            chatJump: ImageView,
            isMe: Boolean,
            expireIn: Long?,
            expireAt: Long?,
            @IdRes id: Int,
        ) {
            chatJump.isVisible = expireIn != null
            if (expireIn != null) {
                chatJump.setImageResource(R.drawable.ic_expire_message)
                expireAt?.fullTime()?.let {
                    TooltipCompat.setTooltipText(chatJump, chatJump.context.getString(R.string.Expired_tip, it))
                }
                (chatJump.layoutParams as ConstraintLayout.LayoutParams).apply {
                    if (isMe) {
                        endToStart = id
                        startToEnd = View.NO_ID
                    } else {
                        endToStart = View.NO_ID
                        startToEnd = id
                    }
                }
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
                                SELECT_COLOR,
                            ) as Int,
                        )
                    }
                }
        }

        protected fun fromJsonQuoteMessage(quoteContent: String?): QuoteMessageItem? {
            quoteContent ?: return null
            return try {
                GsonHelper.customGson.fromJson(quoteContent, QuoteMessageItem::class.java)
            } catch (e: Exception) {
                reportException(e)
                null
            }
        }
    }

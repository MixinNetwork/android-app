package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.Constants
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ViewAppCardBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.initChatMode
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenWidth
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.ui.conversation.holder.ActionsCardHolder
import one.mixin.android.vo.AppCardData
import one.mixin.android.widget.linktext.AutoLinkMode
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class AppCardLayout(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    private val binding = ViewAppCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.root.context.defaultSharedPreferences.getInt(Constants.Account.PREF_TEXT_SIZE, 14).apply {
            if (this != 14) {
                val textSize = this.toFloat()
                binding.chatTime.changeSize(textSize - 4f)
                binding.title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize + 2)
                binding.content.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            }
        }
        binding.content.initChatMode(LINK_COLOR)
        binding.content.setSelectedStateColor(SELECT_COLOR)
        binding.cover.roundTopOrBottom(6.dp.toFloat(), top = true, bottom = false)
        binding.root.updateLayoutParams {
            width = min(340.dp, max(240.dp, (binding.root.context.screenWidth() * 3 / 4)))
        }
    }

    fun setData(
        appCardData: AppCardData, isLast: Boolean, isMe: Boolean, createdAt: String, status: String, isPin: Boolean,
        isRepresentative: Boolean, isSecret: Boolean, onItemListener: MessageAdapter.OnItemListener?, textGestureListener: ActionsCardHolder.TextGestureListener?
    ) {
        bind(appCardData, isLast, isMe, createdAt, status, isPin, isRepresentative, isSecret)
        binding.content.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener?.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION, AutoLinkMode.MODE_BOT -> {
                    onItemListener?.onMentionClick(matchedText)
                }
                AutoLinkMode.MODE_PHONE -> {
                    onItemListener?.onPhoneClick(matchedText)
                }
                AutoLinkMode.MODE_EMAIL -> {
                    onItemListener?.onEmailClick(matchedText)
                }
                else -> {
                }
            }
        }
        binding.content.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
            textGestureListener?.longPressed = true

            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener?.onUrlLongClick(matchedText)
                }

                else -> {
                }
            }
        }
    }

    fun setData(
        appCardData: AppCardData, isLast: Boolean, isMe: Boolean, createdAt: String, status: String, isPin: Boolean,
        isRepresentative: Boolean, isSecret: Boolean, onItemListener: ChatHistoryAdapter.OnItemListener, textGestureListener: one.mixin.android.ui.conversation.chathistory.ActionsCardHolder.TextGestureListener?
    ) {
        bind(appCardData, isLast, isMe, createdAt, status, isPin, isRepresentative, isSecret)
        binding.content.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION, AutoLinkMode.MODE_BOT -> {
                    onItemListener.onMentionClick(matchedText)
                }
                AutoLinkMode.MODE_PHONE -> {
                    onItemListener.onPhoneClick(matchedText)
                }
                AutoLinkMode.MODE_EMAIL -> {
                    onItemListener.onEmailClick(matchedText)
                }
                else -> {
                }
            }
        }
        binding.content.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
            textGestureListener?.longPressed = true

            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlLongClick(matchedText)
                }

                else -> {
                }
            }
        }
    }

    private fun bind(
        appCardData: AppCardData, isLast: Boolean, isMe: Boolean, createdAt: String, status: String, isPin: Boolean,
        isRepresentative: Boolean, isSecret: Boolean
    ) {
        if (!appCardData.coverUrl.isNullOrEmpty()) {
            binding.cover.updateLayoutParams {
                val lp = (this as LayoutParams)
                lp.dimensionRatio = "1:1"
            }
            binding.cover.loadImage(appCardData.coverUrl, R.drawable.bot_default)
        } else if (appCardData.cover != null) {
            binding.cover.updateLayoutParams {
                val lp = (this as LayoutParams)
                Timber.e("radio:${appCardData.cover.radio}")
                lp.dimensionRatio = "${appCardData.cover.radio}:1"
            }
            binding.cover.loadImage(appCardData.cover.url, base64Holder = appCardData.cover.thumbnail)
        } else {
            binding.cover.isVisible = false
        }
        binding.title.isVisible = !appCardData.title.isNullOrEmpty()
        binding.content.isVisible = !appCardData.description.isNullOrEmpty()
        val startPadding = if (isMe) 0 else 7.dp
        val endPadding = if (isMe) {
            if (isLast) 4.dp else 7.dp
        } else 0

        binding.root.setPadding(startPadding, binding.root.paddingTop, endPadding, binding.root.paddingBottom)
        binding.title.text = appCardData.title
        binding.content.text = appCardData.description ?: ""
        binding.chatTime.load(isMe, createdAt, status, isPin, isRepresentative, isSecret)
    }

    fun update(width: Int, marginEnd: Int) {
        binding.root.updateLayoutParams {
            this.width = width
        }
        (binding.cover.layoutParams as MarginLayoutParams).marginEnd = marginEnd
    }
}
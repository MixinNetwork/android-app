package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ViewPinMessageLayoutBinding
import one.mixin.android.event.PinMessageEvent
import one.mixin.android.extension.*
import one.mixin.android.job.RefreshConversationJob
import one.mixin.android.session.Session
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

class PinMessageLayout constructor(context: Context, attrs: AttributeSet) :
    ConstraintLayout(context, attrs) {
    private val binding =
        ViewPinMessageLayoutBinding.inflate(LayoutInflater.from(context), this)
    private val pinIv = binding.pinIv
    private val pinAvatar = binding.pinAvatar
    private val pinTitleTv = binding.pinTitleTv
    private val pinSubtitleTv = binding.pinSubtitleTv
    private val pinContentTv = binding.pinContentTv
    private val pinContent = binding.pinContent
    private val pinClose = binding.pinClose
    val pinCount = binding.pinCount
    val pin = binding.pin

    var conversationId: String? = null
        set(value) {
            field = value
            if (value != null) {
                pinContent.isVisible =
                    context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                        .getBoolean("Pin_$conversationId", true)
            }
        }

    init {
        pinIv.round(dip(3))
    }

    private fun collapse() {
        val cx = pinContent.width
        val cy = pinContent.height / 2
        val initialRadius = pinContent.width.toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(pinContent, cx, cy, initialRadius, 0f)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                pinContent.visibility = View.INVISIBLE
            }
        })

        anim.start()
    }


    private var disposable: Disposable? = null
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        disposable = RxBus.listen(PinMessageEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.conversationId == conversationId) {
                    context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                        .putBoolean("Pin_$conversationId", false)
                    expand()
                }
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.dispose()
    }

    private fun expand() {
        val cx = pinContent.width
        val cy = pinContent.height / 2
        val finalRadius = pinContent.width.toFloat()
        val anim = ViewAnimationUtils.createCircularReveal(pinContent, cx, cy, 0f, finalRadius)
        pinContent.visibility = View.VISIBLE
        anim.start()
    }

    fun bind(message: MessageItem, clickAction: (String) -> Unit) {
        pinClose.setOnClickListener {
            if (pinContent.isVisible) {
                collapse()
            }
            context.sharedPreferences(RefreshConversationJob.PREFERENCES_CONVERSATION)
                .putBoolean("Pin_$conversationId", false)
        }
        pinContent.setOnClickListener {
            clickAction.invoke(message.messageId)
        }
        when {
            message.type == null -> {
                pinIv.visibility = View.GONE
                pinAvatar.visibility = View.GONE
            }
            message.type.endsWith("_TEXT") -> {
                if (message.mentions != null) {
                    pinContentTv.renderMessage(
                        message.content,
                        MentionRenderCache.singleton.getMentionRenderContext(message.mentions)
                    )
                } else {
                    pinContentTv.text = message.content
                }
                pinContentTv.visibility = View.VISIBLE
                pinSubtitleTv.visibility = View.GONE
                pinTitleTv.visibility = View.GONE
                pinIv.visibility = View.GONE
                pinAvatar.visibility = View.GONE
                setIcon()
            }
            message.type == MessageCategory.MESSAGE_RECALL.name -> {
                pinSubtitleTv.setText(
                    if (message.userId == Session.getAccountId()) {
                        R.string.chat_recall_me
                    } else {
                        R.string.chat_recall_delete
                    }
                )
                pinIv.visibility = View.GONE
                pinAvatar.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
                setIcon(R.drawable.ic_type_recall)
            }
            message.type.endsWith("_IMAGE") -> {
                pinIv.loadImageCenterCrop(
                    message.mediaUrl,
                    R.drawable.image_holder
                )
                pinSubtitleTv.setText(R.string.photo)
                setIcon(R.drawable.ic_type_pic)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.VISIBLE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_VIDEO") -> {
                pinIv.loadImageCenterCrop(
                    message.mediaUrl,
                    R.drawable.image_holder
                )
                pinSubtitleTv.setText(R.string.video)
                setIcon(R.drawable.ic_type_video)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.VISIBLE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_LIVE") -> {
                pinIv.loadImageCenterCrop(
                    message.thumbUrl,
                    R.drawable.image_holder
                )
                pinSubtitleTv.setText(R.string.live)
                setIcon(R.drawable.ic_type_live)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.VISIBLE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_DATA") -> {
                message.mediaName.notNullWithElse(
                    {
                        pinSubtitleTv.text = it
                    },
                    {
                        pinSubtitleTv.setText(R.string.document)
                    }
                )
                setIcon(R.drawable.ic_type_file)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_POST") -> {
                pinSubtitleTv.setText(R.string.post)
                setIcon(R.drawable.ic_type_file)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_TRANSCRIPT") -> {
                pinSubtitleTv.setText(R.string.transcript)
                setIcon(R.drawable.ic_type_transcript)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_LOCATION") -> {
                pinSubtitleTv.setText(R.string.location)
                setIcon(R.drawable.ic_type_location)
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_AUDIO") -> {
                message.mediaDuration.notNullWithElse(
                    {
                        pinSubtitleTv.text = it.toLong().formatMillis()
                    },
                    {
                        pinSubtitleTv.setText(R.string.audio)
                    }
                )
                setIcon(R.drawable.ic_type_audio)
                pinIv.visibility = View.GONE
                pinAvatar.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_STICKER") -> {
                pinSubtitleTv.setText(R.string.common_sticker)
                setIcon(R.drawable.ic_type_stiker)
                pinIv.loadImageCenterCrop(
                    message.assetUrl,
                    R.drawable.image_holder
                )
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.VISIBLE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type.endsWith("_CONTACT") -> {
                pinSubtitleTv.text = message.sharedUserIdentityNumber
                setIcon(R.drawable.ic_type_contact)
                pinAvatar.setInfo(
                    message.sharedUserFullName,
                    message.sharedUserAvatarUrl,
                    message.sharedUserId
                        ?: "0"
                )
                pinAvatar.visibility = View.VISIBLE
                pinIv.visibility = View.INVISIBLE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            message.type == MessageCategory.APP_BUTTON_GROUP.name || message.type == MessageCategory.APP_CARD.name -> {
                pinSubtitleTv.setText(R.string.extensions)
                setIcon(R.drawable.ic_type_touch_app)
                pinIv.visibility = View.GONE
                pinAvatar.visibility = View.GONE
                pinContentTv.visibility = View.GONE
                pinSubtitleTv.visibility = View.VISIBLE
                pinTitleTv.visibility = View.VISIBLE
            }
            else -> {
                pinAvatar.visibility = View.GONE
                pinIv.visibility = View.GONE
            }
        }
    }

    private fun setIcon(@DrawableRes icon: Int? = null) {
        icon.notNullWithElse(
            { drawable ->
                AppCompatResources.getDrawable(context, drawable).let {
                    it?.setBounds(0, 0, context.dpToPx(10f), context.dpToPx(10f))
                    TextViewCompat.setCompoundDrawablesRelative(
                        pinSubtitleTv,
                        it,
                        null,
                        null,
                        null
                    )
                }
            },
            {
                TextViewCompat.setCompoundDrawablesRelative(
                    pinSubtitleTv,
                    null,
                    null,
                    null,
                    null
                )
            }
        )
    }
}

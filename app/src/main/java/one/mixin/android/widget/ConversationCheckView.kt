package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.gson.Gson
import one.mixin.android.R
import one.mixin.android.databinding.ViewConversationCheckBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.User
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isCallMessage
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isGroupCall
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.membershipIcon
import one.mixin.android.vo.showVerifiedOrBot

class ConversationCheckView : LinearLayout, Checkable {
    private var checked = false

    private val binding = ViewConversationCheckBinding.inflate(LayoutInflater.from(context), this, true)
    val normal get() = binding.normal
    val avatar get() = binding.avatar
    val verifiedIv get() = binding.verifiedIv
    val botIv get() = binding.botIv

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        setOnClickListener {
            toggle()
        }
    }

    override fun setChecked(b: Boolean) {
        checked = b
        if (b) {
            binding.checkIv.visibility = View.VISIBLE
            binding.layout.setBackgroundColor(selectColor)
        } else {
            binding.checkIv.visibility = View.GONE
            binding.layout.setBackgroundColor(defaultColor)
        }
    }

    private val defaultColor by lazy {
        context.colorFromAttribute(R.attr.bg_white)
    }

    private val selectColor by lazy {
        context.colorFromAttribute(R.attr.bg_gray)
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        if (checkEnabled) {
            isChecked = !isChecked
        }
    }

    fun disableCheck() {
        checkEnabled = false
    }

    private var checkEnabled: Boolean = true

    fun bind(
        item: ConversationMinimal,
        listener: ForwardAdapter.ForwardListener?,
    ) {
        binding.apply {
            if (item.isGroupConversation()) {
                normal.text = item.groupName
                setConversationSubtitle(item)
                avatar.setGroup(item.iconUrl())
            } else {
                normal.text = item.name
                mixinIdTv.text = item.ownerIdentityNumber
                avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
            }
            if (item.isMembership()) {
                membershipIv.setImageResource(item.membership.membershipIcon())
                membershipIv.isVisible = true
            } else {
                membershipIv.isVisible = false
            }
            botIv.isVisible = item.isBot()
            setOnClickListener {
                toggle()
                listener?.onConversationClick(item)
            }
        }
    }

    fun bind(
        item: ConversationMinimal,
        listener: (Boolean) -> Unit,
    ) {
        binding.apply {
            if (item.isGroupConversation()) {
                normal.text = item.groupName
                setConversationSubtitle(item)
                avatar.setGroup(item.iconUrl())
            } else {
                normal.text = item.name
                mixinIdTv.text = item.ownerIdentityNumber
                avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
            }
            botIv.isVisible = item.isBot()
            setOnClickListener {
                toggle()
                listener(isChecked)
            }
        }
    }

    fun bind(
        item: User,
        listener: ForwardAdapter.ForwardListener?,
    ) {
        binding.apply {
            normal.text = item.fullName
            mixinIdTv.text = item.identityNumber
            avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
            setOnClickListener {
                toggle()
                listener?.onUserItemClick(item)
            }
            item.showVerifiedOrBot(verifiedIv, botIv, membershipIv)
        }
    }

    private fun setConversationSubtitle(item: ConversationMinimal) {
        when {
            item.messageStatus == MessageStatus.FAILED.name -> {
                item.content?.let {
                    binding.mixinIdTv.setText(
                        if (item.isSignal()) {
                            R.string.Waiting_for_this_message
                        } else {
                            R.string.chat_decryption_failed
                        },
                    )
                }
            }
            item.messageStatus == MessageStatus.UNKNOWN.name -> {
                item.content?.let {
                    item.content.let {
                        binding.mixinIdTv.setText(R.string.message_not_support)
                    }
                }
            }
            item.isText() -> {
                item.content?.let {
                    binding.mixinIdTv.text = it
                }
            }
            item.contentType == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name -> {
                binding.mixinIdTv.setText(R.string.content_transfer)
            }
            item.contentType == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name -> {
                binding.mixinIdTv.setText(R.string.content_transfer)
            }
            item.isSticker() -> {
                binding.mixinIdTv.setText(R.string.content_sticker)
            }
            item.isImage() -> {
                binding.mixinIdTv.setText(R.string.content_photo)
            }
            item.isVideo() -> {
                binding.mixinIdTv.setText(R.string.content_video)
            }
            item.isLive() -> {
                binding.mixinIdTv.setText(R.string.content_live)
            }
            item.isData() -> {
                binding.mixinIdTv.setText(R.string.content_file)
            }
            item.isPost() -> {
                binding.mixinIdTv.text = MarkwonUtil.parseContent(item.content)
            }
            item.isTranscript() -> {
                binding.mixinIdTv.setText(R.string.content_transcript)
            }
            item.isLocation() -> {
                binding.mixinIdTv.setText(R.string.content_location)
            }
            item.isAudio() -> {
                binding.mixinIdTv.setText(R.string.content_audio)
            }
            item.contentType == MessageCategory.APP_BUTTON_GROUP.name -> {
                val buttons =
                    Gson().fromJson(item.content, Array<ActionButtonData>::class.java)
                var content = ""
                buttons.map { content += "[" + it.label + "]" }
                binding.mixinIdTv.text = content
            }
            item.contentType == MessageCategory.APP_CARD.name -> {
                val cardData =
                    Gson().fromJson(item.content, AppCardData::class.java)
                binding.mixinIdTv.text = "[${cardData.title}]"
            }
            item.isContact() -> {
                binding.mixinIdTv.setText(R.string.content_contact)
            }
            item.isCallMessage() -> {
                binding.mixinIdTv.setText(R.string.content_voice)
            }
            item.isRecall() -> {
                binding.mixinIdTv.text = context.getString(R.string.This_message_was_deleted)
            }
            item.isGroupCall() -> {
                binding.mixinIdTv.setText(R.string.content_group_call)
            }
            item.contentType == MessageCategory.MESSAGE_PIN.name -> {
                binding.mixinIdTv.setText(R.string.content_pin)
            }
            item.contentType == MessageCategory.SYSTEM_CONVERSATION.name -> {
                binding.mixinIdTv.setText(R.string.content_system)
            }
            else -> binding.mixinIdTv.text = ""
        }
    }
}

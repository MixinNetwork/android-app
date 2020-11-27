package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewConversationCheckBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ConversationCheckView : LinearLayout, Checkable {
    private var checked = false

    private val binding = ViewConversationCheckBinding.inflate(LayoutInflater.from(context), this, true)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
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

    fun bind(item: ConversationItem, listener: ForwardAdapter.ForwardListener?) {
        if (item.isGroup()) {
            binding.normal.text = item.groupName
            binding.avatar.setGroup(item.iconUrl())
        } else {
            binding.normal.text = item.name
            binding.avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
        }
        binding.botIv.visibility = if (item.isBot()) View.VISIBLE else View.GONE
        setOnClickListener {
            toggle()
            listener?.onConversationItemClick(item)
        }
    }

    fun bind(item: User, listener: ForwardAdapter.ForwardListener?) {
        binding.normal.text = item.fullName
        binding.avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
        setOnClickListener {
            toggle()
            listener?.onUserItemClick(item)
        }
        item.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
    }
}

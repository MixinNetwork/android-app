package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_conversation_check.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class ConversationCheckView : LinearLayout, Checkable {
    private var checked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.view_conversation_check, this, true)
        setOnClickListener {
            toggle()
        }
    }

    override fun setChecked(b: Boolean) {
        checked = b
        if (b) {
            check_iv.visibility = View.VISIBLE
            layout.setBackgroundColor(selectColor)
        } else {
            check_iv.visibility = View.GONE
            layout.setBackgroundColor(defaultColor)
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
            normal.text = item.groupName
            avatar.setGroup(item.iconUrl())
        } else {
            normal.text = item.name
            avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
        }
        bot_iv.visibility = if (item.isBot()) View.VISIBLE else View.GONE
        setOnClickListener {
            toggle()
            listener?.onConversationItemClick(item)
        }
    }

    fun bind(item: User, listener: ForwardAdapter.ForwardListener?) {
        normal.text = item.fullName
        avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
        setOnClickListener {
            toggle()
            listener?.onUserItemClick(item)
        }
        item.showVerifiedOrBot(verified_iv, bot_iv)
    }
}

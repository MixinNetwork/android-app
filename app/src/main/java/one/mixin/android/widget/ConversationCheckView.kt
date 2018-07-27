package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_conversation_check.view.*
import one.mixin.android.R
import one.mixin.android.ui.forward.ForwardAdapter
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.User

class ConversationCheckView : LinearLayout, Checkable {
    private var checked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

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
            layout.setBackgroundColor(Color.parseColor("#F5F5F5"))
        } else {
            check_iv.visibility = View.GONE
            layout.setBackgroundColor(Color.WHITE)
        }
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        isChecked = !isChecked
    }

    fun bind(item: ConversationItem, listener: ForwardAdapter.ForwardListener?) {
        if (item.isGroup()) {
            normal.text = item.groupName
            avatar.setGroup(item.iconUrl())
        } else {
            normal.text = item.name
            avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerIdentityNumber)
        }
        bot_iv.visibility = if (item.isBot()) View.VISIBLE else View.GONE
        setOnClickListener {
            toggle()
            listener?.onConversationItemClick(item)
        }
    }

    fun bind(item: User, listener: ForwardAdapter.ForwardListener?) {
        normal.text = item.fullName
        avatar.setInfo(item.fullName, item.avatarUrl, item.identityNumber)
        setOnClickListener {
            toggle()
            listener?.onUserItemClick(item)
        }
        bot_iv.visibility = if (item.appId != null) View.VISIBLE else View.GONE
        verified_iv.visibility = if (item.isVerified == true) View.VISIBLE else View.GONE
    }
}

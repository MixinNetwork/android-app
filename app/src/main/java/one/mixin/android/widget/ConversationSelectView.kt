package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewConversationSelectBinding
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.isGroupConversation

class ConversationSelectView : LinearLayout, Checkable {
    private var checked = false

    private val binding = ViewConversationSelectBinding.inflate(LayoutInflater.from(context), this, true)
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
            binding.checkIv.setBackgroundResource(R.drawable.bg_round_check)
        } else {
            binding.checkIv.setBackgroundResource(R.drawable.bg_round_8_solid_dark_gray)
        }
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

    fun bind(item: ConversationMinimal, listener: (Boolean) -> Unit) {
        binding.apply {
            if (item.isGroupConversation()) {
                normal.text = item.groupName
                avatar.setGroup(item.iconUrl())
            } else {
                normal.text = item.name
                avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
            }
            botIv.isVisible = item.isBot()
            setOnClickListener {
                toggle()
                listener(isChecked)
            }
        }
    }
}

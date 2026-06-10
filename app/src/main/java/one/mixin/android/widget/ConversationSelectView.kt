package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewConversationSelectBinding
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.isGroupConversation

class ConversationSelectView : LinearLayout, Checkable {
    private var checked = false

    private val binding = ViewConversationSelectBinding.inflate(LayoutInflater.from(context), this, true)
    val normal get() = binding.normal
    val avatar get() = binding.avatar

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

    fun bind(
        item: ConversationMinimal,
        listener: (Boolean) -> Unit,
    ) {
        binding.apply {
            if (item.isGroupConversation()) {
                avatar.setGroup(item.iconUrl())
            } else {
                avatar.setInfo(item.getConversationName(), item.iconUrl(), item.ownerId)
            }
            normal.setName(item)
            setOnClickListener {
                toggle()
                listener(isChecked)
            }
        }
    }
}

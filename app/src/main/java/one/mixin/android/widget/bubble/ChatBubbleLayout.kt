package one.mixin.android.widget.bubble

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.databinding.LayoutChatBubbleBinding

class ChatBubbleLayout(context: Context, attributeSet: AttributeSet) :
    ConstraintLayout(context, attributeSet) {
    private val binding = LayoutChatBubbleBinding.inflate(LayoutInflater.from(context), this)

    var chatName = binding.chatName
    var chatJump = binding.chatJump
}
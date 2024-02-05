package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.databinding.ItemReceiverBinding
import one.mixin.android.extension.dp
import one.mixin.android.vo.User

class TransferReceiverItem : LinearLayout {

    private val _binding: ItemReceiverBinding
    private val dp8 = 8.dp
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        _binding = ItemReceiverBinding.inflate(LayoutInflater.from(context), this)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(user: User) {
        _binding.apply {
            name.text = "${user.fullName} (${user.identityNumber})"
            userAvatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        }
    }


}
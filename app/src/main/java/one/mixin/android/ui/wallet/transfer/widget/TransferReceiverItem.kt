package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
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
        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = AppCompatResources.getDrawable(context, outValue.resourceId)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(user: User) {
        _binding.apply {
            name.text = "${user.fullName} (${user.identityNumber})"
            userAvatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
            if (user.isMembership()) {
                membershipIv.isVisible = true
                verifiedIv.isVisible = false
                botIv.isVisible = false
            } else if (user.isVerified == true) {
                membershipIv.isVisible = false
                verifiedIv.isVisible = true
                botIv.isVisible = false
            } else if (user.isBot()) {
                membershipIv.isVisible = false
                verifiedIv.isVisible = false
                botIv.isVisible = true
            } else {
                membershipIv.isVisible = false
                verifiedIv.isVisible = false
                botIv.isVisible = false
            }
        }
    }
}

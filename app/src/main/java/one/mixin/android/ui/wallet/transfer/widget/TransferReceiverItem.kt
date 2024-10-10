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
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.databinding.ItemReceiverBinding
import one.mixin.android.extension.dp
import one.mixin.android.vo.User

class TransferReceiverItem : LinearLayout {
    private val _binding: ItemReceiverBinding
    private val dp8 = 8.dp
    private val dp6 = 6.dp
    private val dp4 = 4.dp

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
    fun setContent(user: User, signers: List<String>? = null) {
        _binding.apply {
            if (signers != null) {
                userSelect.isVisible = true
                userSelect.setImageResource(
                    if (signers.contains(user.userId)) {
                        R.drawable.ic_ceceive_selected
                    } else {
                        R.drawable.ic_ceceive_unselected
                    }
                )
                userAvatar.updateLayoutParams<MarginLayoutParams> {
                    marginEnd = dp6
                }
            } else {
                userSelect.isVisible = false
                userAvatar.updateLayoutParams<MarginLayoutParams> {
                    marginEnd = dp4
                }
            }
            name.setName(user, "${user.fullName} (${user.identityNumber})")
            userAvatar.setInfo(user.fullName, user.avatarUrl, user.identityNumber)
        }
    }
}

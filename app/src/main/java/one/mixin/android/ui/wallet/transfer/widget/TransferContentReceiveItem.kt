package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.databinding.ItemTransferReceiveContentBinding
import one.mixin.android.extension.dp
import one.mixin.android.vo.User

class TransferContentReceiveItem : LinearLayout {

    private val _binding: ItemTransferReceiveContentBinding
    private val dp16 = 16.dp
    private val dp8 = 8.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferReceiveContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp16, dp8, dp16, dp8)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(@StringRes titleRes: Int, user: User) {
        _binding.apply {
            title.setText(context.getString(titleRes).uppercase())
            val item = TransferReceiverItem(context)
            item.setContent(user)
            userContainer.addView(item)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setContent(@StringRes titleRes: Int, users: List<User>, sendersThreshold:Int?=null) {
        _binding.apply {
            if (sendersThreshold != null) {
                title.text = "${context.getString(titleRes).uppercase()} (${sendersThreshold}/${users.size})"
            } else {
                title.text = context.getString(titleRes).uppercase()
            }
            users.forEach { user ->
                val item = TransferReceiverItem(context)
                item.setContent(user)
                userContainer.addView(item)
            }
        }
    }
}

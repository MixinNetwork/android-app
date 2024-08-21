package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.api.response.SafeTransactionRecipient
import one.mixin.android.databinding.ItemTransferRecipientBinding
import one.mixin.android.databinding.ItemTransferSafeReceiveContentBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.layoutInflater

class TransferContentSafeReceiveItem : LinearLayout {
    private val _binding: ItemTransferSafeReceiveContentBinding
    private val dp28 = 28.dp
    private val dp8 = 8.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferSafeReceiveContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp28, dp8, dp28, dp8)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        list: List<SafeTransactionRecipient>
    ){
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            list.forEach { recipient->

                val itemView =  ItemTransferRecipientBinding.inflate(context.layoutInflater,this@TransferContentSafeReceiveItem,false)

                if (recipient.label != null) {
                    itemView.labelTextView.text = recipient.label
                    itemView. labelTextView.visibility = View.VISIBLE
                } else {
                    itemView.labelTextView.visibility = View.GONE
                }

                itemView. addressTextView.text = recipient.address
                itemView.amountTextView.text = recipient.amount

                container.addView(itemView.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            }
        }
    }
}

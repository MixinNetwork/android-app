package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.api.response.SafeTransactionRecipient
import one.mixin.android.databinding.ItemTransferRecipientBinding
import one.mixin.android.databinding.ItemTransferSafeReceiveContentBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.layoutInflater
import one.mixin.android.ui.wallet.WalletTransferLabelStyle
import one.mixin.android.widget.linktext.RoundBackgroundColorSpan

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
        list: List<SafeTransactionRecipient>,
        symbol: String,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            list.forEach { recipient ->

                val itemView = ItemTransferRecipientBinding.inflate(context.layoutInflater, this@TransferContentSafeReceiveItem, false)

                itemView.addressTextView.text = createRecipientSpannable(recipient.label, recipient.address)
                itemView.amountTextView.text = "${recipient.amount} $symbol"

                container.addView(itemView.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16.dp
                })
            }
        }
    }

    private fun createRecipientSpannable(label: String?, address: String): SpannableString {
        return if (!label.isNullOrBlank()) {
            val fullText = "$address $label"
            val spannableString = SpannableString(fullText)
            val start = fullText.lastIndexOf(label)
            val end = start + label.length
            val backgroundColor = Color.parseColor(WalletTransferLabelStyle.backgroundColorHex(label))
            val backgroundColorSpan = RoundBackgroundColorSpan(backgroundColor, Color.WHITE)
            spannableString.setSpan(RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(backgroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString
        } else {
            SpannableString(address)
        }
    }
}

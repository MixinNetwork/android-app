package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import one.mixin.android.databinding.ItemTransferContentBinding
import one.mixin.android.extension.dp
import one.mixin.android.widget.linktext.RoundBackgroundColorSpan

class TransferContentItem : LinearLayout {
    private val _binding: ItemTransferContentBinding

    private val dp28 = 28.dp
    private val dp8 = 8.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp28, dp8, dp28, dp8)
    }

    fun setContent(
        @StringRes titleResId: Int,
        contentStr: String,
        foot: String? = null,
    ) {
        _binding.apply {
            title.setText(context.getString(titleResId).uppercase())
            content.text = contentStr
            footer.isVisible = !foot.isNullOrBlank()
            footer.text = foot
        }
    }

    fun setContentAndLabel(
        @StringRes titleResId: Int,
        contentStr: String,
        label: String,
    ) {
        _binding.apply {
            title.setText(context.getString(titleResId).uppercase())
            footer.isVisible = false

            val fullText = "$contentStr $label"

            val spannableString = SpannableString(fullText)

            val start = fullText.lastIndexOf(label)
            val end = start + label.length

            val backgroundColor: Int = Color.parseColor("#8DCC99")
            val backgroundColorSpan = RoundBackgroundColorSpan(backgroundColor, Color.WHITE)
            spannableString.setSpan(RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(backgroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            content.text = spannableString
        }
    }
}

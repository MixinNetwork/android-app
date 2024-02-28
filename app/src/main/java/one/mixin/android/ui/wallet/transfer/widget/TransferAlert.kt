package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferAlertBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.textColor
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.dp

class TransferAlert : FrameLayout {
    private val _binding: ViewTransferAlertBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewTransferAlertBinding.inflate(LayoutInflater.from(context), this)
        setPadding(20.dp, 10.dp, 20.dp, 32.dp)
        setBackgroundColor(context.colorAttr(R.attr.bg_white))
        setOnClickListener { }
    }

    private var tipIndex: Int = 0

    @SuppressLint("SetTextI18n")
    fun warning(
        @DrawableRes iconRes: Int,
        list: List<String>,
    ) {
        _binding.apply {
            icon.setImageResource(iconRes)
            content.textColorResource = R.color.text_color_error_tip
            index.textColorResource = R.color.text_color_error_tip
            positive.setText(R.string.Got_it)
            list.first().let {
                content.text = it
            }
            index.text = "${tipIndex + 1}/${list.size}"
            positive.setOnClickListener {
                tipIndex++
                if (tipIndex == list.size) {
                    isVisible = false
                } else {
                    index.text = "${tipIndex + 1}/${list.size}"
                    content.text = list[tipIndex]
                }
            }
            layout.setBackgroundResource(R.drawable.bg_transfer_alert_warning)
        }
    }

    @SuppressLint("SetTextI18n")
    fun info(
        @DrawableRes iconRes: Int,
        info: String,
        @StringRes positiveText: Int,
        @StringRes negativeText: Int,
        positiveClickLint: OnClickListener,
        negativeClickListener: OnClickListener,
    ) {
        _binding.apply {
            icon.setImageResource(iconRes)
            content.textColor = context.colorFromAttribute(R.attr.text_primary)
            index.textColor = context.colorFromAttribute(R.attr.text_primary)
            index.isVisible = false
            negative.isVisible = true
            positive.setText(positiveText)
            positive.setOnClickListener(positiveClickLint)
            negative.setText(negativeText)
            negative.setOnClickListener(negativeClickListener)
            content.text = info
            layout.setBackgroundResource(R.drawable.bg_transfer_alert_info)
        }
    }
}

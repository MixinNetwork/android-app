package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferAlertBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.textColor
import one.mixin.android.extension.textColorResource

class TransferAlert : ConstraintLayout {

    private val _binding: ViewTransferAlertBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewTransferAlertBinding.inflate(LayoutInflater.from(context), this)
        setOnClickListener {  }
    }

    private var tipIndex: Int = 0

    @SuppressLint("SetTextI18n")
    fun warning(@DrawableRes iconRes: Int, list: List<String>, cancel: OnClickListener) {
        _binding.apply {
            icon.setImageResource(iconRes)
            content.textColorResource = R.color.text_color_error_tip
            index.textColorResource = R.color.text_color_error_tip
            positive.setText(R.string.Cancel)
            positive.setOnClickListener(cancel)
            negative.setText(R.string.Confirm)
            list.first().let {
                content.text = it
            }
            index.text = "${tipIndex + 1}/${list.size}"
            negative.setOnClickListener {
                tipIndex++
                if (tipIndex == list.size) {
                    isVisible = false
                } else {
                    index.text = "${tipIndex + 1}/${list.size}"
                    content.text = list[tipIndex]
                }
            }
            setBackgroundResource(R.drawable.bg_transfer_alert_warning)
        }
    }

    @SuppressLint("SetTextI18n")
    fun info(
        @DrawableRes iconRes: Int, info: String, @StringRes positiveText: Int, @StringRes negativeText: Int,
        positiveClickLint: OnClickListener, negativeClickListener: OnClickListener,
    ) {
        _binding.apply {
            icon.setImageResource(iconRes)
            content.textColor = context.colorFromAttribute(R.attr.text_primary)
            index.textColor = context.colorFromAttribute(R.attr.text_primary)
            index.isVisible = false
            positive.setText(positiveText)
            positive.setOnClickListener(positiveClickLint)
            negative.setText(negativeText)
            negative.setOnClickListener(negativeClickListener)
            content.text = info
            setBackgroundResource(R.drawable.bg_transfer_alert_info)
        }
    }
}
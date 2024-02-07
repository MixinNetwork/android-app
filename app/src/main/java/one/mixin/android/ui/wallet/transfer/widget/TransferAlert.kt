package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferAlertBinding
import one.mixin.android.extension.textColor
import one.mixin.android.extension.textColorResource

class TransferAlert : ConstraintLayout {

    private val _binding: ViewTransferAlertBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewTransferAlertBinding.inflate(LayoutInflater.from(context), this)
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

    fun info() {
        _binding.apply {
            icon.setImageResource(R.drawable.ic_transfer_fingerprint)
            positive.text = "Done"
            negative.text = "Enable"
            content.text = "Do you want to turn on fingerprint payment? After opening, the fingerprint can be verified to complete the payment quickly when the money is transferred."
            setBackgroundResource(R.drawable.bg_transfer_alert_info)
        }
    }
}
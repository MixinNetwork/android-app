package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferAlertBinding
class TransferAlert : ConstraintLayout {

    private val _binding: ViewTransferAlertBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewTransferAlertBinding.inflate(LayoutInflater.from(context), this)
        _binding.apply {
            // Todo
            icon.setImageResource(R.drawable.ic_transfer_fingerprint)
            positive.text = "Done"
            negative.text = "Enable"
            content.text = "Do you want to turn on fingerprint payment? After opening, the fingerprint can be verified to complete the payment quickly when the money is transferred."
            setBackgroundResource(R.drawable.bg_transfer_alert_info)
        }
    }

}
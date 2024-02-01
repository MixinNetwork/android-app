package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.databinding.ViewTransferAlertBinding
class TransferAlert : ConstraintLayout {

    private val _binding: ViewTransferAlertBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewTransferAlertBinding.inflate(LayoutInflater.from(context), this)
    }

}
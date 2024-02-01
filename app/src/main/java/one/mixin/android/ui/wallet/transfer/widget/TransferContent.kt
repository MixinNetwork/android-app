package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.LayoutMenuBinding
import one.mixin.android.databinding.ViewTransferContentBinding

class TransferContent : LinearLayout {

    private val _binding: ViewTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferContentBinding.inflate(LayoutInflater.from(context), this)
        _binding.apply {
            // Todo remove test code
            amount.setContent(R.string.Amount, "1.01 BTC")
            address.setContent(R.string.Address, "0x10fab41d2caCF05E3CE2123450Bda4AF8806F480")
            network.setContent(R.string.network, "Ethereum (ERC-20)")
            networkFee.setContent(R.string.network_fee, "0.01 BTC", "$10")
        }
    }
}
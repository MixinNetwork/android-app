package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.databinding.ItemTransferContentBinding

class TransferContentItem : LinearLayout {

    private val _binding: ItemTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferContentBinding.inflate(LayoutInflater.from(context), this)
    }
}
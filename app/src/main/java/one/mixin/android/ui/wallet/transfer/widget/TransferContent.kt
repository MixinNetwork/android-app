package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.databinding.LayoutMenuBinding
import one.mixin.android.databinding.ViewTransferContentBinding

class TransferContent : LinearLayout {

    private val _binding: ViewTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferContentBinding.inflate(LayoutInflater.from(context), this)

    }
}
package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransactionActionsBinding

class TransactionActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewTransactionActionsBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        setBackgroundResource(R.drawable.bg_round_light_gray_12dp)
    }

    val speedUp get() = binding.speedUpTv
    val cancelTx get() = binding.cancelTxTv
}

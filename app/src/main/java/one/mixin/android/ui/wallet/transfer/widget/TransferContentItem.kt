package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import one.mixin.android.databinding.ItemTransferContentBinding
import one.mixin.android.extension.dp

class TransferContentItem : LinearLayout {

    private val _binding: ItemTransferContentBinding
    private val dp16 = 16.dp
    private val dp8 = 8.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp16, dp8, dp16, dp8)
    }

    fun setContent(@StringRes titleResId: Int, contentStr: String, foot: String? = null) {
        _binding.apply {
            title.setText(titleResId)
            content.text = contentStr
            footer.isVisible = !foot.isNullOrBlank()
            footer.text = foot
        }
    }
}
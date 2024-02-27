package one.mixin.android.ui.tip.wc

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewWalletCreateHeaderBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.textColorResource

class CreateHeader : LinearLayout {
    private val _binding: ViewWalletCreateHeaderBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewWalletCreateHeaderBinding.inflate(LayoutInflater.from(context), this)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun progress() {
        _binding.apply {
            iconLayout.displayedChild = 2
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            title.setText(R.string.Creating)
            subTitle.setText(R.string.wallet_create_eth_title)
        }
    }

    fun filed(
        errorMessage: String?,
    ) {
        _binding.apply {
            iconLayout.displayedChild = 1
            statusIcon.setImageResource(R.drawable.ic_transfer_status_failed)
            subTitle.text = errorMessage
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            title.setText(R.string.wallet_create_failed)
        }
    }

    fun success() {
        _binding.apply {
            iconLayout.displayedChild = 1
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            statusIcon.setImageResource(R.drawable.ic_transfer_status_success)

            title.setText(R.string.wallet_create_successfully)
            subTitle.setText(R.string.wallet_create_eth_title)
        }
    }

    fun awaiting() {
        _binding.apply {
            iconLayout.displayedChild = 0

            title.setText(R.string.wallet_create_eth)
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            subTitle.textColorResource = R.color.text_color_error_tip
        }
    }
}

package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferHeaderBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.vo.safe.TokenItem

class TransferHeader : LinearLayout {

    private val _binding: ViewTransferHeaderBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferHeaderBinding.inflate(LayoutInflater.from(context), this)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun setContent(@StringRes titleResId: Int, @StringRes subTitleResId: Int, asset: TokenItem) {
        _binding.apply {
            title.setText(titleResId)
            subTitle.setText(subTitleResId)
            assetIcon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetIcon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        }
    }

    fun progress() {
        _binding.icon.displayedChild = 2
    }

    fun filed() {
        _binding.icon.displayedChild = 1
        _binding.statusIcon.setImageResource(R.drawable.ic_transfer_status_failed)
    }

    fun success() {
        _binding.icon.displayedChild = 1
        _binding.statusIcon.setImageResource(R.drawable.ic_transfer_status_success)
    }
}
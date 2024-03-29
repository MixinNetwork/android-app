package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.databinding.ViewChainCardBinding
import one.mixin.android.extension.formatPublicKey

class ChainCard : FrameLayout {
    private val _binding: ViewChainCardBinding
    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewChainCardBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.bg_chain_gradient)
    }

    fun setContent(
        title: String,
        subTitle: String,
        @DrawableRes icon: Int,
        onClickListener: OnClickListener
    ) {
        _binding.title.text = title
        _binding.subTitle.text = subTitle
        _binding.icon.setImageResource(icon)
        _binding.actionTv.setOnClickListener(onClickListener)
        _binding.actionTv.setText(R.string.Unlock)
    }

    fun setContent(
        title: String,
        address: String,
        @StringRes action: Int,
        @DrawableRes icon: Int,
        onClickListener: OnClickListener
    ) {
        _binding.title.text = title
        _binding.subTitle.text = address.formatPublicKey()
        _binding.icon.setImageResource(icon)
        _binding.actionTv.setText(action)
        _binding.actionTv.setOnClickListener(onClickListener)
    }

}

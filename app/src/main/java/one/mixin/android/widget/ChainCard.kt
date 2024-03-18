package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewChainCardBinding
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import java.math.BigDecimal

class ChainCard: FrameLayout {


    private val _binding: ViewChainCardBinding
    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewChainCardBinding.inflate(LayoutInflater.from(context), this)
        setBackgroundResource(R.drawable.bg_chain_gradient)
    }

    fun setContent(title: String, subTitle: String, @DrawableRes icon: Int) {
        _binding.title.isVisible = true
        _binding.subTitle.isVisible = true
        _binding.address.isVisible = false
        _binding.amount.isVisible = false
        _binding.title.text = title
        _binding.subTitle.text = subTitle
        _binding.icon.setImageResource(icon)
    }

    fun setContent(address: String, amount: BigDecimal, @DrawableRes icon: Int) {
        _binding.title.isVisible = false
        _binding.subTitle.isVisible = false
        _binding.address.isVisible = true
        _binding.amount.isVisible = true
        _binding.address.text = address.formatPublicKey()
        _binding.amount.text = amount.numberFormat8()
        _binding.icon.setImageResource(icon)
        _binding.createTv.text = "View Polygon Account"
    }

    fun setOnCreateListener(onClickListener: OnClickListener) {
        _binding.createTv.setOnClickListener(onClickListener)
    }
}
package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.databinding.ViewWeb3TokenHeaderBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class Web3TokenHeader : ConstraintLayout {
    private val _binding: ViewWeb3TokenHeaderBinding
    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewWeb3TokenHeaderBinding.inflate(LayoutInflater.from(context), this)
        _binding.send.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.receive.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.more.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
    }

    private var onClickAction: ((Int) -> Unit)? = null

    fun setOnClickAction(onClickAction: ((Int) -> Unit)? = null) {
        this.onClickAction = onClickAction
    }

    fun setToken(token: Web3Token) {
        _binding.avatar.bg.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
        _binding.avatar.badge.loadImage(token.chainIconUrl, R.drawable.ic_avatar_place_holder)
        _binding.totalTv.text =
            try {
                if (token.balance.numberFormat().toFloat() == 0f) {
                    "0.00"
                } else {
                    token.balance.numberFormat()
                }
            } catch (ignored: NumberFormatException) {
                token.balance.numberFormat()
            }
        _binding.value.text = "≈ ${Fiats.getSymbol()}${(BigDecimal(token.price).multiply(BigDecimal(token.balance)).multiply(BigDecimal(Fiats.getRate())).numberFormat2())}"
        _binding.symbol.text = token.symbol
    }
}

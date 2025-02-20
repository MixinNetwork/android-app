package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.api.response.isSolToken
import one.mixin.android.databinding.ViewWeb3TokenHeaderBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.home.web3.StakeAccountSummary
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
        _binding.swap.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.stake.root.updateLayoutParams<LayoutParams> {
            topToBottom = _binding.send.id
        }
        _binding.stake.iconVa.displayedChild = 0
    }

    fun enableSwap() {
        _binding.apply {
            swap.isInvisible = false
        }
    }

    private var onClickAction: ((Int) -> Unit)? = null

    fun setOnClickAction(onClickAction: ((Int) -> Unit)? = null) {
        this.onClickAction = onClickAction
    }

    @SuppressLint("SetTextI18n")
    fun setToken(token: Web3Token) {
        _binding.avatar.bg.loadImage(token.iconUrl, R.drawable.ic_avatar_place_holder)
        _binding.avatar.badge.loadImage(token.chainIconUrl, R.drawable.ic_avatar_place_holder)
        _binding.totalTv.text =
            try {
                if (token.balance.toFloat() == 0f) {
                    "0.00"
                } else {
                    token.balance
                }
            } catch (ignored: Exception) {
                token.balance
            }
        _binding.value.text = runCatching { "≈ ${Fiats.getSymbol()}${(BigDecimal(token.price).multiply(BigDecimal(token.balance)).multiply(BigDecimal(Fiats.getRate())).numberFormat2())}" }.getOrDefault("N/A")
        _binding.symbol.text = token.symbol
        _binding.stake.root.isVisible = token.isSolToken()
    }

    @SuppressLint("SetTextI18n")
    fun showStake(stakeAccountSummary: StakeAccountSummary?) {
        _binding.stake.apply {
            if (stakeAccountSummary == null) {
                _binding.stake.iconVa.displayedChild = 0
                amountTv.text = "0 SOL"
                countTv.text = "0 account"
            } else {
                _binding.stake.iconVa.displayedChild = 1
                amountTv.text = "${stakeAccountSummary.amount} SOL"
                countTv.text = "${stakeAccountSummary.count} account"
                stakeRl.setOnClickListener {
                    onClickAction?.invoke(_binding.stake.stakeRl.id)
                }
            }
        }
    }
}

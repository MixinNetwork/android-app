package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
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
        _binding.more.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.stake.root.apply {
            isVisible = false
            updateLayoutParams<LayoutParams> {
                topToBottom = _binding.more.id
            }
        }
    }

    fun enableSwap() {
        _binding.apply {
            (more.layoutParams as LayoutParams).apply {
                startToEnd = R.id.swap
            }
            swap.isVisible = true
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
        _binding.value.text = "≈ ${Fiats.getSymbol()}${(BigDecimal(token.price).multiply(BigDecimal(token.balance)).multiply(BigDecimal(Fiats.getRate())).numberFormat2())}"
        _binding.symbol.text = token.symbol
    }

    @SuppressLint("SetTextI18n")
    fun showStake(stakeAccountSummary: StakeAccountSummary?) {
        if (stakeAccountSummary == null) {
            _binding.stake.root.isVisible = false
        } else {
            _binding.stake.apply {
                root.isVisible = true
                icon.loadImage("https://raw.githubusercontent.com/solana-labs/token-list/main/assets/mainnet/So11111111111111111111111111111111111111112/logo.png")
                amountTv.text = "${stakeAccountSummary.amount} SOL"
                countTv.text = "${stakeAccountSummary.count} account"
                stakeRl.setOnClickListener {
                    onClickAction?.invoke(_binding.stake.stakeRl.id)
                }
            }
        }
    }
}

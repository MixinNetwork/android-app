package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.databinding.ViewWeb3HeaderBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.home.web3.StakeAccountSummary
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class Web3Header : ConstraintLayout {
    private val _binding: ViewWeb3HeaderBinding
    constructor(context: Context) : this(context, null)

    @SuppressLint("CustomViewStyleable")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewWeb3HeaderBinding.inflate(LayoutInflater.from(context), this)
        _binding.send.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.receive.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.browser.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.more.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.swap.setOnClickListener {
            onClickAction?.invoke(it.id)
        }
        _binding.stake.root.isVisible = false
        _binding.stake.root.updateLayoutParams<LayoutParams> {
            topToBottom = _binding.more.id
        }
    }

    private var onClickAction: ((Int) -> Unit)? = null

    fun setOnClickAction(onClickAction: ((Int) -> Unit)? = null) {
        this.onClickAction = onClickAction
    }

    fun setText(string: String) {
        _binding.totalTv.text = (BigDecimal(string).multiply(BigDecimal(Fiats.getRate())).numberFormat2())
        _binding.symbol.text = Fiats.getSymbol()
    }

    fun setTitle(titleRes: Int) {
        _binding.title.setText(titleRes)
    }

    fun enableSwap() {
        _binding.apply {
            (browser.layoutParams as LayoutParams).apply {
                startToEnd = R.id.swap
            }
            (receive.layoutParams as LayoutParams).apply {
                endToStart = R.id.swap
            }
            swap.isVisible = true
        }
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
            }
        }
    }
}

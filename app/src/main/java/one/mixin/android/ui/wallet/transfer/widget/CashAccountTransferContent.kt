package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewCashAccountTransferContentBinding
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class CashAccountTransferContent : LinearLayout {
    private val binding: ViewCashAccountTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        binding = ViewCashAccountTransferContentBinding.inflate(LayoutInflater.from(context), this)
    }

    fun render(item: TransferBiometricItem) {
        val asset = item.asset ?: return
        val receiveAmount = item.cashReceiveAmount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val balance = item.cashBalance?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val symbol = item.cashReceiveSymbol.orEmpty()
        val receiveText = "+${receiveAmount.numberFormat2()} $symbol"
        val totalBalanceText = "${balance.plus(receiveAmount).numberFormat2()} $symbol"
        val description = context.getString(R.string.cash_account_preview_description, receiveText, totalBalanceText)
        val fiatAmount = (item.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) * asset.priceFiat()

        binding.receiveAmount.text = receiveText
        binding.description.text = description.highlightAmounts(receiveText, totalBalanceText)
        binding.payWithValue.text = context.getString(
            R.string.cash_account_preview_pay_with_value,
            item.amount.numberFormat8(),
            asset.symbol,
            Fiats.getSymbol(),
            fiatAmount.numberFormat2(),
        )
        binding.feeValue.text = "${Fiats.getSymbol()}0"
    }

    fun setOnCloseClickListener(listener: OnClickListener) {
        binding.close.setOnClickListener(listener)
    }

    private fun String.highlightAmounts(vararg amounts: String): SpannableString {
        val spannable = SpannableString(this)
        val green = ContextCompat.getColor(context, R.color.wallet_green)
        amounts.forEach { amount ->
            val start = indexOf(amount)
            if (start >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(green),
                    start,
                    start + amount.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        return spannable
    }
}

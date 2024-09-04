package one.mixin.android.ui.home.web3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.SafeAccount
import one.mixin.android.databinding.ViewMarketCardBinding
import one.mixin.android.databinding.ViewTransferContentBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.textColorResource
import one.mixin.android.session.Session
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.getChainName
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import java.math.BigDecimal

class MarketCard : LinearLayout {
    private val _binding: ViewMarketCardBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewMarketCardBinding.inflate(LayoutInflater.from(context), this)
    }

    @SuppressLint("SetTextI18n")
    fun render(@StringRes title: Int, value: String, percentage: BigDecimal) {
        _binding.apply {
            titleTv.setText(title)
            valueTv.text = capFormat(value, BigDecimal(Fiats.getRate()), Fiats.getSymbol())
            extraTv.text = "${percentage.numberFormat2()}%"
            if (percentage < BigDecimal.ZERO) {
                extraTv.textColorResource = R.color.wallet_pink
            } else {
                extraTv.textColorResource = R.color.wallet_green
            }
        }
    }

    private fun capFormat(vol: String, rate: BigDecimal, symbol: String): String {
        val formatVol = try {
            BigDecimal(vol).multiply(rate).numberFormatCompact()
        } catch (e: NumberFormatException) {
            null
        }
        if (formatVol != null) {
            return "$symbol$formatVol"
        }
        return context.getString(R.string.N_A)
    }

    @SuppressLint("SetTextI18n")
    fun render(@StringRes title: Int, percentage: BigDecimal, extra: String) {
        _binding.apply {
            titleTv.setText(title)
            valueTv.text = "${percentage.numberFormat2()}%"
            extraTv.text = extra
            extraTv.setTextColor(context.colorAttr(R.attr.text_minor))
        }
    }
}

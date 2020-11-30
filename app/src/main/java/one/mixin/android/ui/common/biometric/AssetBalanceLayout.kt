package one.mixin.android.ui.common.biometric

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.LayoutAssetBalanceBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class AssetBalanceLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    private val binding = LayoutAssetBalanceBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    @SuppressLint("SetTextI18n")
    fun setInfo(t: BiometricItem) {
        val asset = t.asset
        val amount = t.amount
        binding.apply {
            assetIcon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetIcon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            val balanceText = amount.numberFormat() + " " + asset.symbol
            balance.text = balanceText
            if (t is WithdrawBiometricItem) {
                val amountText = "${context.getString(R.string.amount)} $balanceText"
                val feeText = "${context.getString(R.string.fee)} ${t.fee.numberFormat()} ${asset.chainSymbol}"
                balanceAs.text = "$amountText ${getValueText(amount, asset.priceFiat())}\n$feeText ${getValueText(t.fee, asset.chainPriceFiat())}"
            } else {
                balanceAs.text = getValueText(amount, asset.priceFiat())
            }
        }
    }

    private fun getValueText(value: String, assetPrice: BigDecimal) =
        "≈ ${Fiats.getSymbol()}${(
            BigDecimal(value) *
                assetPrice
            ).numberFormat2()}"
}

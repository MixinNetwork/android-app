package one.mixin.android.ui.common.biometric

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.text.bold
import androidx.core.view.isVisible
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
    fun setInfo(t: AssetBiometricItem) {
        val asset = t.asset
        val amount = t.amount
        binding.apply {
            assetIcon.isVisible = true
            avatar.isVisible = false
            assetIcon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetIcon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            val balanceText = amount.numberFormat() + " " + asset.symbol
            balance.text = balanceText
            if (t is WithdrawBiometricItem) {
                val subText = SpannableStringBuilder()
                    .append(context.getString(R.string.Amount))
                    .append(" ")
                    .bold { append(balanceText) }
                    .append(" ")
                    .append(getValueText(amount, asset.priceFiat()))
                    .append("\n")
                    .append(context.getString(R.string.Fee))
                    .append(" ")
                    .bold { append(t.fee.numberFormat()).append(" ").append(asset.chainSymbol).append(" ") }
                    .append(getValueText(t.fee, asset.chainPriceFiat()))
                balanceAs.text = subText
            } else {
                balanceAs.text = getValueText(amount, asset.priceFiat())
            }
        }
    }

    fun setInfoWithUser(t: TransferBiometricItem) {
        binding.apply {
            avatar.isVisible = true
            assetIcon.isVisible = false
            val u = t.user
            avatar.setInfo(u.fullName, u.avatarUrl, u.userId)
            balance.text = u.fullName
            balanceAs.text = context.getString(R.string.contact_mixin_id, u.identityNumber)
        }
    }

    private fun getValueText(value: String, assetPrice: BigDecimal) =
        "≈ ${Fiats.getSymbol()}${(
            BigDecimal(value) *
                assetPrice
            ).numberFormat2()}"
}

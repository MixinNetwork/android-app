package one.mixin.android.ui.common.biometric

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import java.math.BigDecimal
import kotlinx.android.synthetic.main.layout_asset_balance.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem

class AssetBalanceLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {
    init {
        LayoutInflater.from(context).inflate(R.layout.layout_asset_balance, this, true)
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    @SuppressLint("SetTextI18n")
    fun setInfo(asset: AssetItem, amount: String) {
        asset_icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        asset_icon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        balance.text = amount.numberFormat() + " " + asset.symbol
        balance_as.text = "≈ ${(BigDecimal(amount) *
            asset.priceFiat()).numberFormat2()} ${Session.getFiatCurrency()}"
    }
}

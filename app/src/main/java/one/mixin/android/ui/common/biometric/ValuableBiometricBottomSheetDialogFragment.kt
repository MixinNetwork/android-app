package one.mixin.android.ui.common.biometric

import java.math.BigDecimal
import one.mixin.android.R
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats

abstract class ValuableBiometricBottomSheetDialogFragment<T : AssetBiometricItem> : BiometricBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"
    }

    private val assetBalance by lazy {
        contentView.findViewById<AssetBalanceLayout?>(R.id.asset_balance)
    }

    override fun onResume() {
        super.onResume()
        assetBalance?.parent?.requestLayout()
    }

    protected fun setBiometricItem() {
        val t = getBiometricItem()
        assetBalance?.setInfo(t)
        checkState(t)
    }

    protected fun getDescription(): String {
        val t = getBiometricItem()
        val asset = t.asset ?: return ""
        val pre = "${t.amount} ${asset.symbol}"
        val post = "≈ ${Fiats.getSymbol()}${(BigDecimal(t.amount) * asset.priceFiat()).numberFormat2()}"
        return "$pre ($post)"
    }

    abstract fun checkState(t: BiometricItem)

    abstract fun getBiometricItem(): T
}

package one.mixin.android.ui.common.biometric

import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

abstract class ValuableBiometricBottomSheetDialogFragment<T : BiometricItem> : BiometricBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"
    }

    override fun onResume() {
        super.onResume()
        biometricBinding.assetBalance.parent.requestLayout()
    }

    protected fun setBiometricItem() {
        val t = getBiometricItem()
        biometricBinding.assetBalance.setInfo(t)
        checkState(t)
    }

    protected fun getDescription(): String {
        val t = getBiometricItem()
        val pre = "${t.amount} ${t.asset.symbol}"
        val post = "≈ ${Fiats.getSymbol()}${(BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()}"
        return "$pre ($post)"
    }

    abstract fun checkState(t: BiometricItem)

    abstract fun getBiometricItem(): T
}

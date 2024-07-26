package one.mixin.android.ui.oldwallet.biometric

import one.mixin.android.R
import one.mixin.android.extension.formatTo2DecimalsWithCommas
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

abstract class ValuableBiometricBottomSheetDialogFragment<T : AssetBiometricItem> : BiometricBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"
    }

    private val assetBalance by lazy {
        contentView.findViewById<AssetBalanceLayout>(R.id.asset_balance)
    }

    override fun onResume() {
        super.onResume()
        assetBalance.parent.requestLayout()
    }

    protected fun setBiometricItem() {
        val t = getBiometricItem()
        assetBalance.setInfo(t)
        checkState(t)
    }

    protected fun getDescription(): String {
        val t = getBiometricItem()
        val pre = "${t.amount} ${t.asset.symbol}"
        val post = "≈ ${Fiats.getSymbol()}${(BigDecimal(t.amount) * t.asset.priceFiat()).formatTo2DecimalsWithCommas()}"
        return "$pre ($post)"
    }

    abstract fun checkState(t: BiometricItem)

    abstract fun getBiometricItem(): T
}

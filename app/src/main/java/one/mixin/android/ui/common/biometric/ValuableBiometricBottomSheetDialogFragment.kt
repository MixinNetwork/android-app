package one.mixin.android.ui.common.biometric

import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

abstract class ValuableBiometricBottomSheetDialogFragment<T : BiometricItem> : BiometricBottomSheetDialogFragment() {
    companion object {
        const val ARGS_BIOMETRIC_ITEM = "args_biometric_item"
    }

    override fun onResume() {
        super.onResume()
        contentView.asset_balance.parent.requestLayout()
    }

    protected fun setBiometricItem() {
        val t = getBiometricItem()
        contentView.asset_balance.setInfo(t)
        checkState(t.state)
    }

    protected fun getDescription(): String {
        val t = getBiometricItem()
        val pre = "${t.amount} ${t.asset.symbol}"
        val post = "≈ ${Fiats.getSymbol()}${(BigDecimal(t.amount) * t.asset.priceFiat()).numberFormat2()}"
        return "$pre ($post)"
    }

    abstract fun checkState(state: String)

    abstract fun getBiometricItem(): T
}

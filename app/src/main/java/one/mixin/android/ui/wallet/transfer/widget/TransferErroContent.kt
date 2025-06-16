package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.SafeAccount
import one.mixin.android.databinding.ViewTransferContentBinding
import one.mixin.android.databinding.ViewTransferErrorContenBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.session.Session
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.getChainName
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.MixinInvoice
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
import one.mixin.android.vo.toUser
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.JsSigner
import java.math.BigDecimal

class TransferErrorContent : LinearLayout {
    private val _binding: ViewTransferErrorContenBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferErrorContenBinding.inflate(LayoutInflater.from(context), this)
    }

    fun renderAsset(assetBiometricItem: AssetBiometricItem, extra: TokensExtra?) {
        _binding.apply {
            amount.isVisible = true
            amount.setContent(R.string.Amount, "${assetBiometricItem.amount} ${assetBiometricItem.asset?.symbol}", amountAs(assetBiometricItem.amount, assetBiometricItem.asset!!))
            total.isVisible = true

            network.setContent(R.string.network, getChainName(assetBiometricItem.asset!!.chainId, assetBiometricItem.asset!!.chainName, assetBiometricItem.asset!!.assetKey) ?: "")
            networkFee.isVisible = true
            if (assetBiometricItem is WithdrawBiometricItem) {
                val (totalAmount, totalPrice) = formatWithdrawBiometricItem(assetBiometricItem)
                total.setContent(R.string.Total, totalAmount, totalPrice)

                val fee = assetBiometricItem.fee!!
                networkFee.isVisible = true
                networkFee.setContent(R.string.Fee, "${fee.fee} ${fee.token.symbol}", amountAs(fee.fee, fee.token))
            } else {
                networkFee.setContent(R.string.Fee, "0 ${assetBiometricItem.asset?.symbol}", amountAs("0", assetBiometricItem.asset!!))
                total.setContent(R.string.Total, "${assetBiometricItem.amount} ${assetBiometricItem.asset?.symbol}", amountAs(assetBiometricItem.amount, assetBiometricItem.asset!!))
            }

            sender.isVisible = false
            balance.isVisible = true
            balance.setContent(R.string.Available_Balance, "${extra?.balance?.numberFormat8() ?: "0"} ${assetBiometricItem.asset?.symbol ?: ""}", amountAs(extra?.balance ?: "0", assetBiometricItem.asset!!))

        }
    }


    private fun formatWithdrawBiometricItem(withdrawBiometricItem: WithdrawBiometricItem): Pair<String, String> {
        val asset = withdrawBiometricItem.asset!!
        val feeAsset = withdrawBiometricItem.fee!!.token
        val amount = withdrawBiometricItem.amount
        val feeAmount = withdrawBiometricItem.fee!!.fee
        val value =
            try {
                if (amount.toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(amount)
                }
            } catch (e: ArithmeticException) {
                BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        val feeValue =
            try {
                if (feeAmount.toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(feeAmount)
                }
            } catch (e: ArithmeticException) {
                BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        if (asset.assetId == feeAsset.assetId) {
            val totalAmount = value.plus(feeValue)
            val total = asset.priceFiat() * totalAmount
            return Pair("${totalAmount.numberFormat8()} ${asset.symbol}", "${total.numberFormat2()} ${Fiats.getAccountCurrencyAppearance()}")
        } else {
            val total = asset.priceFiat() * value + feeAsset.priceFiat() * feeValue
            return Pair("${withdrawBiometricItem.amount} ${asset.symbol} + $feeAmount ${feeAsset.symbol}", "${total.numberFormat2()} ${Fiats.getAccountCurrencyAppearance()}")
        }
    }

    fun renderAsset(asset: Web3TokenItem, amount: BigDecimal,fee: BigDecimal) {
        _binding.apply {
            balance.isVisible = true
            balance.setContent(R.string.Available_Balance, "${asset.balance.numberFormat12()} ${asset.symbol}", amountAs(asset.balance, asset))
            networkFee.isVisible = true
            networkFee.setContent(R.string.Network_Fee, "${fee.numberFormat12()} ${asset.symbol}", amountAs(fee.toPlainString(), asset))
            sender.isVisible = true
            if (amount != BigDecimal.ZERO) {
                total.isVisible = true
                total.setContent(R.string.Total, "${(amount + fee).numberFormat12()} ${asset.symbol}", amountAs((amount + fee).toPlainString(), asset))
            } else {
                total.isVisible = false
            }
            sender.setContent(R.string.Sender, JsSigner.evmAddress)
            network.setContent(R.string.network, getChainName(asset.chainId, asset.chainName, asset.assetKey) ?: "")
        }
    }

    private fun amountAs(
        amount: String,
        asset: TokenItem,
    ): String {
        val value =
            try {
                if (asset.priceFiat().toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(amount) * asset.priceFiat()
                }
            } catch (e: ArithmeticException) {
                BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        return "${Fiats.getSymbol()}${value.numberFormat2()}"
    }

    private fun amountAs(
        amount: String,
        asset: Web3TokenItem,
    ): String {
        val value =
            try {
                if (asset.priceFiat().toDouble() == 0.0) {
                    BigDecimal.ZERO
                } else {
                    BigDecimal(amount) * asset.priceFiat()
                }
            } catch (e: ArithmeticException) {
                BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        return "${Fiats.getSymbol()}${value.numberFormat2()}"
    }
}

package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferErrorContenBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.numberFormat12
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.util.getChainName
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.safe.TokensExtra
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

    fun renderAsset(assetBiometricItem: AssetBiometricItem, extra: TokensExtra?, feeExtra: TokensExtra? = null) {
        _binding.apply {
            if (assetBiometricItem is WithdrawBiometricItem && assetBiometricItem.isBalanceEnough(
                    extra?.balance,
                    feeExtra?.balance
                ) == 3
            ) {
                val fee = assetBiometricItem.fee?.token ?: return
                amount.isVisible = true
                amount.setContent(
                    R.string.Fee,
                    "${assetBiometricItem.fee?.fee?.numberFormat8()} ${fee.symbol}",
                    amountAs(assetBiometricItem.fee?.fee ?: "0", fee)
                )
                network.setContent(
                    R.string.network,
                    getChainName(
                        fee.chainId,
                        fee.chainName,
                        fee.assetKey
                    ) ?: ""
                )
                balance.isVisible = true
                balance.setContent(
                    R.string.Balance,
                    "${feeExtra?.balance?.numberFormat8() ?: "0"} ${fee.symbol ?: ""}",
                    amountAs(feeExtra?.balance ?: "0", fee)
                )
            } else {
                val asset = assetBiometricItem.asset?:return
                amount.isVisible = true
                amount.setContent(
                    R.string.Amount,
                    "${assetBiometricItem.amount} ${asset?.symbol}",
                    amountAs(assetBiometricItem.amount, asset!!)
                )
                network.setContent(
                    R.string.network,
                    getChainName(
                        asset!!.chainId,
                        asset!!.chainName,
                        asset!!.assetKey
                    ) ?: ""
                )
                if (assetBiometricItem is WithdrawBiometricItem && assetBiometricItem.fee?.token != null && asset?.assetId == assetBiometricItem.fee?.token?.assetId) {
                    val (totalAmount, totalPrice) = formatWithdrawBiometricItem(assetBiometricItem)
                    total.isVisible = true
                    total.setContent(R.string.Total, totalAmount, totalPrice)

                    val fee = assetBiometricItem.fee!!
                    networkFee.isVisible = true
                    networkFee.setContent(
                        R.string.Fee,
                        "${fee.fee} ${fee.token.symbol}",
                        amountAs(fee.fee, fee.token)
                    )
                } else {
                    networkFee.isVisible = false
                    total.isVisible = false
                }

                sender.isVisible = false
                balance.isVisible = true
                balance.setContent(
                    R.string.Balance,
                    "${extra?.balance?.numberFormat8() ?: "0"} ${asset?.symbol ?: ""}",
                    amountAs(extra?.balance ?: "0", asset!!)
                )
            }
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
            balance.setContent(R.string.Balance, "${asset.balance.numberFormat12()} ${asset.symbol}", amountAs(asset.balance, asset))
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

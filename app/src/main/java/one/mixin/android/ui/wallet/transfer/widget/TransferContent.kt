package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferContentBinding
import one.mixin.android.extension.numberFormat2
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.getChainName
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

class TransferContent : LinearLayout {
    private val _binding: ViewTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferContentBinding.inflate(LayoutInflater.from(context), this)
    }

    fun render(transferItem: BiometricItem) {
        when (transferItem) {
            is TransferBiometricItem -> {
                renderTransfer(transferItem)
            }

            is AddressTransferBiometricItem -> {
                renderAddressTransfer(transferItem)
            }

            is WithdrawBiometricItem -> {
                renderWithdrawTransfer(transferItem)
            }

            is AddressManageBiometricItem -> {
                renderAddressManage(transferItem)
            }
        }
    }

    fun render(
        safeMultisigsBiometricItem: SafeMultisigsBiometricItem,
        sender: List<User>,
        receiver: List<User>,
        userClick: (User) -> Unit,
    ) {
        renderMultisigsTransfer(safeMultisigsBiometricItem, sender, receiver, userClick)
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
        return "${value.numberFormat2()} ${Fiats.getAccountCurrencyAppearance()}"
    }

    private fun renderTransfer(transferBiometricItem: TransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}", "${amountAs(transferBiometricItem.amount, transferBiometricItem.asset!!)}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = true
            receive.setContent(R.string.Receive, transferBiometricItem.users) {}
            addressReceive.isVisible = true
            addressReceive.setContent(R.string.RECEIVER_WILL_RECEIVE, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}")

            networkFee.isVisible = true
            networkFee.setContent(R.string.network_fee, "0 ${transferBiometricItem.asset?.symbol}")

            if (!transferBiometricItem.memo.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Memo, transferBiometricItem.memo ?: "")
            }

            val tokenItem = transferBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    private fun renderAddressManage(addressManageBiometricItem: AddressManageBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Label, addressManageBiometricItem.label ?: "")
            address.isVisible = true
            addressReceive.isVisible = false
            receive.isVisible = false
            address.setContent(R.string.Address, addressManageBiometricItem.destination ?: "")
            val tokenItem = addressManageBiometricItem.asset!!
            val addressMemo = addressManageBiometricItem.tag
            if (addressMemo.isNullOrBlank()) {
                memo.isVisible = false
            } else {
                memo.isVisible = true
                memo.setContent(
                    if (tokenItem.assetId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                        R.string.Tag
                    } else {
                        R.string.withdrawal_memo
                    },
                    addressMemo,
                )
            }

            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    private fun renderMultisigsTransfer(
        safeMultisigsBiometricItem: SafeMultisigsBiometricItem,
        senders: List<User>,
        receiver: List<User>,
        userClick: (User) -> Unit,
    ) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}", "${amountAs(safeMultisigsBiometricItem.amount, safeMultisigsBiometricItem.asset!!)}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = true
            receive.setContent(R.string.Receive, receiver, null, userClick)
            sender.isVisible = true
            sender.setContent(R.string.Senders, senders, safeMultisigsBiometricItem.sendersThreshold, userClick)
            addressReceive.isVisible = true
            addressReceive.setContent(R.string.RECEIVER_WILL_RECEIVE, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}")

            networkFee.isVisible = true
            networkFee.setContent(R.string.network_fee, "0 ${safeMultisigsBiometricItem.asset?.symbol}")

            if (!safeMultisigsBiometricItem.memo.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Memo, safeMultisigsBiometricItem.memo ?: "")
            }

            val tokenItem = safeMultisigsBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    private fun renderAddressTransfer(addressTransferBiometricItem: AddressTransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol}", "${amountAs(addressTransferBiometricItem.amount, addressTransferBiometricItem.asset!!)}")
            address.isVisible = false
            receive.isVisible = false
            addressReceive.isVisible = true
            addressReceive.setContent(R.string.ADDRESS_WILL_RECEIVE, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol}")

            val tokenItem = addressTransferBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    private fun renderWithdrawTransfer(withdrawBiometricItem: WithdrawBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${withdrawBiometricItem.amount} ${withdrawBiometricItem.asset?.symbol}", "${amountAs(withdrawBiometricItem.amount, withdrawBiometricItem.asset!!)}")
            address.isVisible = false
            receive.isVisible = false
            address.isVisible = true

            val label = withdrawBiometricItem.label
            if (label != null) {
                address.setContentAndLabel(R.string.Address, withdrawBiometricItem.displayAddress(), withdrawBiometricItem.label)
            } else {
                address.setContent(R.string.Address, withdrawBiometricItem.displayAddress())
            }
            addressReceive.isVisible = true
            addressReceive.setContent(R.string.ADDRESS_WILL_RECEIVE, "${withdrawBiometricItem.amount} ${withdrawBiometricItem.asset?.symbol}")
            val fee = withdrawBiometricItem.fee!!
            networkFee.isVisible = true
            networkFee.setContent(R.string.network_fee, "${fee.fee} ${fee.token.symbol}")

            val tokenItem = withdrawBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }
}

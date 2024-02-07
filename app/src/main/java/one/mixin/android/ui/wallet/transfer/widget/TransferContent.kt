package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferContentBinding
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.getChainName
import one.mixin.android.vo.User

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
        }
    }

    fun render(safeMultisigsBiometricItem: SafeMultisigsBiometricItem, sender: List<User>, receiver: List<User>) {
        renderMultisigsTransfer(safeMultisigsBiometricItem, sender, receiver)
    }

    private fun renderTransfer(transferBiometricItem: TransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = true
            receive.setContent(R.string.Receive, transferBiometricItem.users.first())
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

    private fun renderMultisigsTransfer(safeMultisigsBiometricItem: SafeMultisigsBiometricItem, senders: List<User>, receiver: List<User>) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = true
            receive.setContent(R.string.Receive, receiver)
            sender.isVisible = true
            sender.setContent(R.string.Senders, senders)
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
            amount.setContent(R.string.Amount, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol} ")
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
            amount.setContent(R.string.Amount, "${withdrawBiometricItem.amount} ${withdrawBiometricItem.asset?.symbol}")
            address.isVisible = false
            receive.isVisible = false
            address.isVisible = true
            address.setContent(R.string.Address, withdrawBiometricItem.displayAddress())
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

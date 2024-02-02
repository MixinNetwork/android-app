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
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem

class TransferContent : LinearLayout {

    private val _binding: ViewTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferContentBinding.inflate(LayoutInflater.from(context), this)
        _binding.apply {
            // Todo remove test code
            amount.setContent(R.string.Amount, "1.01 BTC")
            address.setContent(R.string.Address, "0x10fab41d2caCF05E3CE2123450Bda4AF8806F480")
            network.setContent(R.string.network, "Ethereum (ERC-20)")
            networkFee.setContent(R.string.network_fee, "0.01 BTC", "$10")
        }
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

    private fun renderTransfer(transferBiometricItem: TransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = true
            receive.setContent(transferBiometricItem.users.first())
        }
    }

    private fun renderAddressTransfer(addressTransferBiometricItem: AddressTransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = false
        }
    }

    private fun renderWithdrawTransfer(withdrawBiometricItem: WithdrawBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${withdrawBiometricItem.amount} ${withdrawBiometricItem.asset?.symbol}")
            address.isVisible = false
            addressReceive.isVisible = false
            receive.isVisible = false
        }
    }
}
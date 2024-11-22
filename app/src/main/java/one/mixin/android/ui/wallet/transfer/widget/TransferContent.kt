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
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.session.Session
import one.mixin.android.ui.common.biometric.AddressManageBiometricItem
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.BiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.SafeMultisigsBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.displayAddress
import one.mixin.android.util.getChainName
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import java.math.BigDecimal

class TransferContent : LinearLayout {
    private val _binding: ViewTransferContentBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferContentBinding.inflate(LayoutInflater.from(context), this)
    }

    fun render(
        transferItem: BiometricItem,
        userClick: (User) -> Unit,
    ) {
        when (transferItem) {
            is TransferBiometricItem -> {
                renderTransfer(transferItem, userClick)
            }

            is NftBiometricItem -> {
                renderTransferNft(transferItem, userClick)
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
        if (safeMultisigsBiometricItem.safe != null) {
            renderSafeMultisigsTransfer(safeMultisigsBiometricItem, safeMultisigsBiometricItem.safe)
        } else {
            renderMultisigsTransfer(safeMultisigsBiometricItem, sender, receiver, userClick)
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

    private fun renderTransfer(
        transferBiometricItem: TransferBiometricItem,
        userClick: (User) -> Unit,
    ) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}", amountAs(transferBiometricItem.amount, transferBiometricItem.asset!!))
            address.isVisible = false
            receive.isVisible = true
            sender.isVisible = true
            sender.setContent(R.string.Sender, Session.getAccount()!!.toUser()) {}
            receive.setContent(R.plurals.Receiver_title, transferBiometricItem.users, transferBiometricItem.threshold.toInt(), userClick)
            total.isVisible = true
            total.setContent(R.string.Total, "${transferBiometricItem.amount} ${transferBiometricItem.asset?.symbol}", amountAs(transferBiometricItem.amount, transferBiometricItem.asset!!))

            networkFee.isVisible = true
            networkFee.setContent(R.string.Fee, "0 ${transferBiometricItem.asset?.symbol}", amountAs("0", transferBiometricItem.asset!!))

            if (!transferBiometricItem.memo.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Memo, transferBiometricItem.memo ?: "")
            }

            val tokenItem = transferBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    fun displayHash(transactionHash: String?) {
        _binding.apply {
            hash.isVisible = !transactionHash.isNullOrBlank()
            hash.setContent(R.string.transaction_Hash, transactionHash?:"", selectable = true)
        }
    }

    private fun renderTransferNft(
        nftBiometricItem: NftBiometricItem,
        userClick: (User) -> Unit,
    ) {
        _binding.apply {
            amount.isVisible = false
            address.isVisible = false
            name.isVisible = true
            name.setContent(R.string.Collectible, "${nftBiometricItem.inscriptionCollection.name} #${nftBiometricItem.inscriptionItem.sequence}")
            if (nftBiometricItem.release) {
                receive.isVisible = false
                sender.isVisible = false
                token.isVisible = true
                token.setContent(R.string.Token, "${nftBiometricItem.amount} ${nftBiometricItem.asset?.symbol}", amountAs(nftBiometricItem.amount, nftBiometricItem.asset!!), nftBiometricItem.asset)
                networkFee.isVisible = true
                networkFee.setContent(R.string.Fee, "0")
            } else {
                receive.isVisible = true
                sender.isVisible = true
                sender.setContent(R.string.Sender, Session.getAccount()!!.toUser()) {}
                receive.setContent(R.plurals.Receiver_title, nftBiometricItem.receivers, null, userClick)
            }

            total.isVisible = false
            total.setContent(R.string.Total, "${nftBiometricItem.amount} ${nftBiometricItem.asset?.symbol}", amountAs(nftBiometricItem.amount, nftBiometricItem.asset!!))

            if (!nftBiometricItem.memo.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Memo, nftBiometricItem.memo ?: "")
            }

            network.isVisible = false
        }
    }

    private fun renderAddressManage(addressManageBiometricItem: AddressManageBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Label, addressManageBiometricItem.label ?: "")
            sender.isVisible = true
            sender.setContent(R.string.Sender, Session.getAccount()!!.toUser()) {}
            address.isVisible = true
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
            amount.setContent(R.string.Amount, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}", amountAs(safeMultisigsBiometricItem.amount, safeMultisigsBiometricItem.asset!!))
            receive.isVisible = true

            receive.setContent(R.plurals.Receiver_title, receiver, safeMultisigsBiometricItem.receiverThreshold, userClick)
            sender.isVisible = true
            sender.setContent(R.plurals.Sender_title, senders, safeMultisigsBiometricItem.sendersThreshold, userClick, safeMultisigsBiometricItem.signers)
            total.isVisible = true
            total.setContent(R.string.Total, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}", amountAs(safeMultisigsBiometricItem.amount, safeMultisigsBiometricItem.asset!!))

            networkFee.isVisible = true
            networkFee.setContent(R.string.Fee, "0 ${safeMultisigsBiometricItem.asset?.symbol}", amountAs("0", safeMultisigsBiometricItem.asset!!))

            if (!safeMultisigsBiometricItem.memo.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Memo, safeMultisigsBiometricItem.memo ?: "")
            }

            val tokenItem = safeMultisigsBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    fun updateSenders(
        safeMultisigsBiometricItem: SafeMultisigsBiometricItem,
        senders: List<User>,
        userClick: (User) -> Unit,
    ) {
        _binding.apply {
            sender.setContent(R.plurals.Sender_title, senders, safeMultisigsBiometricItem.sendersThreshold, userClick, safeMultisigsBiometricItem.signers)
        }
    }

    private fun renderSafeMultisigsTransfer(
        safeMultisigsBiometricItem: SafeMultisigsBiometricItem,
        safeAccount: SafeAccount,
    ) {
        _binding.apply {
            amount.setContent(R.string.Total_Amount, "${safeMultisigsBiometricItem.amount} ${safeMultisigsBiometricItem.asset?.symbol}", amountAs(safeMultisigsBiometricItem.amount, safeMultisigsBiometricItem.asset!!), token = safeMultisigsBiometricItem.asset)
            receive.isVisible = false
            sender.isVisible = false
            total.isVisible = false
            networkFee.isVisible = false

            if (!safeAccount.operation.transaction.note.isNullOrBlank()) {
                memo.isVisible = true
                memo.setContent(R.string.Note, safeAccount.operation.transaction.note)
            }
            safeReceives.setContent(R.string.Receiver, safeAccount.operation.transaction.recipients, safeMultisigsBiometricItem.asset?.symbol?:"")
            safeReceives.isVisible = true
            safeSender.setContent(R.string.Sender, safeAccount.address, selectable = true)
            safeSender.isVisible = true
            safe.setContent(R.string.SAFE, safeAccount.name)
            safe.isVisible = true
            network.isVisible = false
        }
    }

    private fun renderAddressTransfer(addressTransferBiometricItem: AddressTransferBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol}", amountAs(addressTransferBiometricItem.amount, addressTransferBiometricItem.asset!!))
            address.isVisible = true
            address.setContent(R.string.Receiver, addressTransferBiometricItem.address)
            total.isVisible = true
            total.setContent(R.string.Total, "${addressTransferBiometricItem.amount} ${addressTransferBiometricItem.asset?.symbol}", amountAs(addressTransferBiometricItem.amount, addressTransferBiometricItem.asset!!))

            val tokenItem = addressTransferBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }

    private fun renderWithdrawTransfer(withdrawBiometricItem: WithdrawBiometricItem) {
        _binding.apply {
            amount.setContent(R.string.Amount, "${withdrawBiometricItem.amount} ${withdrawBiometricItem.asset?.symbol}", amountAs(withdrawBiometricItem.amount, withdrawBiometricItem.asset!!))
            receive.isVisible = false
            address.isVisible = true
            sender.isVisible = true
            total.isVisible = true

            val label = withdrawBiometricItem.label
            if (label != null) {
                address.setContentAndLabel(R.string.Receiver, withdrawBiometricItem.displayAddress(), withdrawBiometricItem.label)
            } else {
                address.setContent(R.string.Receiver, withdrawBiometricItem.displayAddress())
            }

            sender.setContent(R.string.Sender, Session.getAccount()!!.toUser()) {}

            val (totalAmount, totalPrice) = formatWithdrawBiometricItem(withdrawBiometricItem)
            total.setContent(R.string.Total, totalAmount, totalPrice)

            val fee = withdrawBiometricItem.fee!!
            networkFee.isVisible = true
            networkFee.setContent(R.string.Fee, "${fee.fee} ${fee.token.symbol}", amountAs(fee.fee, fee.token))

            val tokenItem = withdrawBiometricItem.asset!!
            network.setContent(R.string.network, getChainName(tokenItem.chainId, tokenItem.chainName, tokenItem.assetKey) ?: "")
        }
    }
}

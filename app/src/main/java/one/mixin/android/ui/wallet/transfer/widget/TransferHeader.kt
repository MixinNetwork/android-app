package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferHeaderBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import one.mixin.android.extension.textColorResource
import one.mixin.android.ui.wallet.transfer.data.TransferType
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.safe.TokenItem

class TransferHeader : LinearLayout {
    private val _binding: ViewTransferHeaderBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewTransferHeaderBinding.inflate(LayoutInflater.from(context), this)
        _binding.nftIcon.round(12.dp)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun progress(type: TransferType) {
        _binding.apply {
            icon.displayedChild = 3
            subTitle.setTextColor(context.colorAttr(R.attr.text_minor))
            when (type) {
                TransferType.transfer, TransferType.nft, TransferType.addressTransfer -> {
                    title.setText(R.string.Sending_Transfer_Request)
                    subTitle.setText(R.string.transfer_sending_description)
                }

                TransferType.nftRelease -> {
                    title.setText(R.string.collectible_releasing)
                    subTitle.setText(R.string.collectible_releasing_description)
                }

                TransferType.withdraw -> {
                    title.setText(R.string.Sending_Withdrawal_Request)
                    subTitle.setText(R.string.withdrawal_sending_description)
                }

                TransferType.addAddress -> {
                    title.setText(R.string.Adding_Address)
                    subTitle.setText(R.string.review_address_hint)
                }

                TransferType.deleteAddress -> {
                    title.setText(R.string.Deleting_Address)
                    subTitle.setText(R.string.address_deleting_description)
                }

                TransferType.mutlSign -> {
                    title.setText(R.string.Sending_Multisig_Signature)
                    subTitle.setText(R.string.multisig_signing_description)
                }

                TransferType.unMulSign -> {
                    title.setText(R.string.Revoking_Multisig_Signature)
                    subTitle.setText(R.string.multisig_unlocking_description)
                }

                TransferType.approve -> {
                    title.setText(R.string.Approving_Transaction)
                    subTitle.setText(R.string.multisig_signing_description)
                }

                TransferType.reject -> {
                    title.setText(R.string.Rejecting_Transaction)
                    subTitle.setText(R.string.multisig_unlocking_description)
                }

                else -> {

                }
            }
        }
    }

    fun filed(
        type: TransferType,
        errorMessage: String?,
    ) {
        _binding.apply {
            icon.displayedChild = 2
            statusIcon.setImageResource(R.drawable.ic_transfer_status_failed)
            subTitle.text = errorMessage
            subTitle.textColorResource = R.color.text_color_error_tip
            when (type) {
                TransferType.transfer, TransferType.nft, TransferType.addressTransfer -> {
                    title.setText(R.string.Transfer_Failed)
                }

                TransferType.nftRelease -> {
                    title.setText(R.string.collectible_release_failed)
                }

                TransferType.withdraw -> {
                    title.setText(R.string.Withdrawal_Failed)
                }

                TransferType.addAddress -> {
                    title.setText(R.string.Adding_Address_Failed)
                }

                TransferType.deleteAddress -> {
                    title.setText(R.string.Deleting_Address_Failed)
                }

                TransferType.mutlSign -> {
                    title.setText(R.string.Multisig_Signing_Failed)
                }

                TransferType.unMulSign -> {
                    title.setText(R.string.Revoking_Multisig_Failed)
                }

                else-> {

                }
            }
        }
    }

    fun success(type: TransferType) {
        _binding.apply {
            icon.displayedChild = 2
            subTitle.setTextColor(context.colorAttr(R.attr.text_minor))
            statusIcon.setImageResource(R.drawable.ic_transfer_status_success)
            when (type) {
                TransferType.transfer, TransferType.nft, TransferType.addressTransfer -> {
                    title.setText(R.string.Transfer_Success)
                    subTitle.setText(R.string.transfer_sent_description)
                }

                TransferType.nftRelease -> {
                    title.setText(R.string.collectible_release_success)
                    subTitle.setText(R.string.collectible_released_description)
                }

                TransferType.withdraw -> {
                    title.setText(R.string.Withdrawal_confirmation)
                    subTitle.setText(R.string.withdrawal_sent_description)
                }

                TransferType.addAddress -> {
                    title.setText(R.string.Confirm_Adding_Address)
                    subTitle.setText(R.string.address_added_description)
                }

                TransferType.deleteAddress -> {
                    title.setText(R.string.Confirm_Deleting_Address)
                    subTitle.setText(R.string.address_deleted_description)
                }

                TransferType.mutlSign -> {
                    title.setText(R.string.Multisig_Transaction)
                    subTitle.setText(R.string.multisig_signed_description)
                }

                TransferType.unMulSign -> {
                    title.setText(R.string.Multisig_Revoked)
                    subTitle.setText(R.string.multisig_unlocked_description)
                }

                TransferType.signed -> {
                    title.setText(R.string.Multisig_Signing_Successful)
                    subTitle.setText(R.string.multisig_signing_successful_description)
                }
                TransferType.approve -> {
                    title.setText(R.string.Transaction_Approved)
                    subTitle.setText(R.string.multisig_state_signed)
                }
                TransferType.reject -> {
                    title.setText(R.string.Transaction_Rejected)
                    subTitle.setText(R.string.multisig_state_unlocked)
                }
            }
        }
    }

    fun awaiting(
        type: TransferType,
        inscriptionItem: InscriptionItem,
        asset: TokenItem,
    ) {
        _binding.apply {
            nftIcon.render(inscriptionItem, asset.iconUrl)
        }
        awaiting(type, asset)
    }

    fun awaiting(
        type: TransferType,
        asset: TokenItem,
    ) {
        _binding.apply {
            icon.displayedChild =
                if (type == TransferType.approve || type == TransferType.reject) {
                    statusIcon.setImageResource(R.drawable.ic_transfer_approve)
                    2
                } else if (type == TransferType.nft || type == TransferType.nftRelease) {
                    1
                } else {
                    0
                }
            when (type) {
                TransferType.transfer, TransferType.nft, TransferType.addressTransfer -> {
                    title.setText(R.string.Transfer_confirmation)
                    subTitle.setText(R.string.review_transfer_hint)
                }

                TransferType.nftRelease -> {
                    title.setText(R.string.collectible_release_confirmation)
                    subTitle.setText(R.string.collectible_release_hint)
                }

                TransferType.withdraw -> {
                    title.setText(R.string.Withdrawal_confirmation)
                    subTitle.setText(R.string.review_withdrawal_hint)
                }

                TransferType.addAddress -> {
                    title.setText(R.string.Confirm_Adding_Address)
                    subTitle.setText(R.string.review_address_hint)
                }

                TransferType.deleteAddress -> {
                    title.setText(R.string.Confirm_Deleting_Address)
                    subTitle.setText(R.string.delete_address_description)
                }

                TransferType.mutlSign -> {
                    title.setText(R.string.Multisig_Transaction)
                    subTitle.setText(R.string.review_transfer_hint)
                }

                TransferType.unMulSign -> {
                    title.setText(R.string.Revoke_Multisig_Signature)
                    subTitle.setText(R.string.review_transfer_hint)
                }

                TransferType.approve -> {
                    title.setText(R.string.Approve_Transaction)
                    subTitle.text = context.getString(R.string.signature_request_from, context.getString(R.string.SAFE))
                }

                TransferType.reject -> {
                    title.setText(R.string.Reject_Transaction)
                    subTitle.text = context.getString(R.string.signature_request_from, context.getString(R.string.SAFE))
                }

                else -> {

                }
            }
            subTitle.setTextColor(context.colorAttr(R.attr.text_minor))
            assetIcon.loadToken(asset)
        }
    }
}

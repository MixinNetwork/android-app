package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal
import kotlinx.android.synthetic.main.fragment_transaction_bottom.view.*
import kotlinx.android.synthetic.main.fragment_transaction_bottom.view.ph
import kotlinx.android.synthetic.main.fragment_transaction_bottom.view.title_view
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColorResource

class TransactionBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransactionFragment"
        const val ARGS_SNAPSHOT = "args_snapshot"
        const val ARGS_ASSET_ID = "args_asset_id"
        const val ARGS_SNAPSHOT_ID = "args_snapshot_id"

        fun newInstance(
            snapshotItem: SnapshotItem? = null,
            asset: AssetItem? = null,
            assetId: String? = null,
            snapshotId: String? = null
        ) = TransactionBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_SNAPSHOT, snapshotItem)
            putParcelable(ARGS_ASSET, asset)
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelable<SnapshotItem>(ARGS_SNAPSHOT) }
    private val asset: AssetItem? by lazy { requireArguments().getParcelable<AssetItem>(ARGS_ASSET) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transaction_bottom, null).apply { isClickable = true }
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        contentView.title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        contentView.title_view.right_animator.visibility = View.GONE
        if (snapshot == null || asset == null) {
            if (snapshotId != null && assetId != null) {
                lifecycleScope.launch {
                    val asset = walletViewModel.simpleAssetItem(assetId!!)
                    val snapshot = walletViewModel.snapshotLocal(assetId!!, snapshotId!!)
                    if (asset == null || snapshot == null) {
                        context?.toast(R.string.error_data)
                    } else {
                        updateUI(asset, snapshot)
                    }
                }
            } else {
                context?.toast(R.string.error_data)
            }
        } else {
            updateUI(asset!!, snapshot!!)
        }
        (dialog as BottomSheet).apply {
            setCustomView(contentView)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(asset: AssetItem, snapshot: SnapshotItem) {
        if (!isAdded) return

        val isPositive = try {
            snapshot.amount.toFloat() > 0
        } catch (e: NumberFormatException) {
            false
        }
        contentView.avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        contentView.avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        contentView.value_tv.text = if (isPositive) "+${snapshot.amount.numberFormat()}"
        else snapshot.amount.numberFormat()
        contentView.symbol_tv.text = asset.symbol
        contentView.value_tv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
        val amount = (BigDecimal(snapshot.amount) * asset.priceFiat()).priceFormat()
        contentView.value_as_tv.text = "≈ ${Fiats.getSymbol()}$amount"
        contentView.transaction_id_tv.text = snapshot.snapshotId
        contentView.transaction_type_tv.text = getSnapshotType(snapshot.type)
        contentView.memo_tv.text = snapshot.memo
        contentView.date_tv.text = snapshot.createdAt.fullDate()
        when (snapshot.type) {
            SnapshotType.deposit.name -> {
                contentView.sender_title.text = getString(R.string.sender)
                contentView.sender_tv.text = snapshot.sender
                contentView.receiver_title.text = getString(R.string.transaction_hash)
                contentView.receiver_tv.text = snapshot.transactionHash
            }
            SnapshotType.transfer.name -> {
                if (isPositive) {
                    contentView.sender_tv.text = snapshot.opponentFullName
                    contentView.receiver_tv.text = Session.getAccount()!!.full_name
                } else {
                    contentView.sender_tv.text = Session.getAccount()!!.full_name
                    contentView.receiver_tv.text = snapshot.opponentFullName
                }
            }
            else -> {
                if (!asset.tag.isNullOrEmpty()) {
                    contentView.receiver_title.text = getString(R.string.account_name)
                } else {
                    contentView.receiver_title.text = getString(R.string.receiver)
                }
                contentView.sender_title.text = getString(R.string.transaction_hash)
                contentView.sender_tv.text = snapshot.transactionHash
                contentView.receiver_tv.text = snapshot.receiver
            }
        }
    }

    private fun getSnapshotType(type: String): String {
        val s = when (type) {
            SnapshotType.transfer.name -> R.string.transfer
            SnapshotType.deposit.name -> R.string.wallet_bottom_deposit
            SnapshotType.withdrawal.name -> R.string.withdrawal
            SnapshotType.fee.name -> R.string.fee
            SnapshotType.rebate.name -> R.string.rebate
            SnapshotType.raw.name -> R.string.filters_raw
            // SnapshotType.pending can NOT access this page
            else -> R.string.not_any
        }
        return requireContext().getString(s)
    }
}

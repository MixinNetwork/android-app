package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_transaction.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.textColorResource

class TransactionFragment : BaseFragment() {
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
        ) = TransactionFragment().withArgs {
            putParcelable(ARGS_SNAPSHOT, snapshotItem)
            putParcelable(ARGS_ASSET, asset)
            putString(ARGS_ASSET_ID, assetId)
            putString(ARGS_SNAPSHOT_ID, snapshotId)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    private val snapshot: SnapshotItem? by lazy { requireArguments().getParcelable<SnapshotItem>(ARGS_SNAPSHOT) }
    private val asset: AssetItem? by lazy { requireArguments().getParcelable<AssetItem>(ARGS_ASSET) }
    private val assetId: String? by lazy { requireArguments().getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { requireArguments().getString(ARGS_SNAPSHOT_ID) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transaction, container, false).apply {
            isClickable = true
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.visibility = View.GONE
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
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(asset: AssetItem, snapshot: SnapshotItem) {
        if (!isAdded) return

        val isPositive = try {
            snapshot.amount.toFloat() > 0
        } catch (e: NumberFormatException) {
            false
        }
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        value_tv.text = if (isPositive) "+${snapshot.amount.numberFormat()}"
            else snapshot.amount.numberFormat()
        symbol_tv.text = asset.symbol
        value_tv.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
        val amount = (BigDecimal(snapshot.amount) * asset.priceFiat()).priceFormat()
        value_as_tv.text = "≈ ${Fiats.getSymbol()}$amount"
        transaction_id_tv.text = snapshot.snapshotId
        transaction_type_tv.text = getSnapshotType(snapshot.type)
        memo_tv.text = snapshot.memo
        date_tv.text = snapshot.createdAt.fullDate()
        when (snapshot.type) {
            SnapshotType.deposit.name -> {
                sender_title.text = getString(R.string.sender)
                sender_tv.text = snapshot.sender
                receiver_title.text = getString(R.string.transaction_hash)
                receiver_tv.text = snapshot.transactionHash
            }
            SnapshotType.transfer.name -> {
                if (isPositive) {
                    sender_tv.text = snapshot.opponentFullName
                    receiver_tv.text = Session.getAccount()!!.full_name
                } else {
                    sender_tv.text = Session.getAccount()!!.full_name
                    receiver_tv.text = snapshot.opponentFullName
                }
            }
            else -> {
                if (!asset.tag.isNullOrEmpty()) {
                    receiver_title.text = getString(R.string.account_name)
                } else {
                    receiver_title.text = getString(R.string.receiver)
                }
                sender_title.text = getString(R.string.transaction_hash)
                sender_tv.text = snapshot.transactionHash
                receiver_tv.text = snapshot.receiver
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

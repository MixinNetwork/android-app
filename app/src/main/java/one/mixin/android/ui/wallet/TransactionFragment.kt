package one.mixin.android.ui.wallet

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_transaction.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.textColorResource
import org.jetbrains.anko.uiThread
import java.math.BigDecimal
import javax.inject.Inject

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
    private val walletViewModel: WalletViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(WalletViewModel::class.java)
    }

    private val snapshot: SnapshotItem? by lazy { arguments!!.getParcelable<SnapshotItem>(ARGS_SNAPSHOT) }
    private val asset: AssetItem? by lazy { arguments!!.getParcelable<AssetItem>(ARGS_ASSET) }
    private val assetId: String? by lazy { arguments!!.getString(ARGS_ASSET_ID) }
    private val snapshotId: String? by lazy { arguments!!.getString(ARGS_SNAPSHOT_ID) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_transaction, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        if (snapshot == null || asset == null) {
            doAsync {
                val asset = walletViewModel.simpleAssetItem(assetId!!)
                val snapshot = walletViewModel.snapshotLocal(assetId!!, snapshotId!!)
                uiThread {
                    if (asset == null || snapshot == null) {
                        context?.toast(R.string.error_unknown)
                    } else {
                        updateUI(asset, snapshot)
                    }
                }
            }
        } else {
            updateUI(asset!!, snapshot!!)
        }
    }

    private fun updateUI(asset: AssetItem, snapshot: SnapshotItem) {
        val isPositive = snapshot.amount.toFloat() > 0
        avatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        avatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
        value_tv.text = if (isPositive) "+${snapshot.amount.numberFormat()} ${asset.symbol}"
        else "${snapshot.amount.numberFormat()} ${asset.symbol}"
        value_tv.textColorResource = if (isPositive) R.color.colorGreen else R.color.colorRed
        val amount = (BigDecimal(snapshot.amount) * BigDecimal(asset.priceUsd)).numberFormat2()
        value_as_tv.text = getString(R.string.wallet_unit_usd, "≈ $amount")
        transaction_id_tv.text = snapshot.snapshotId
        transaction_type_tv.text = getSnapshotType(snapshot.type)
        asset_name_tv.text = asset.name
        if (asset.accountName != null) {
            account_name_ll.visibility = VISIBLE
            asset_name_tv.text = asset.accountName
        }
        memo_tv.text = snapshot.memo
        date_tv.text = snapshot.createdAt.fullDate()
        when {
            snapshot.type == SnapshotType.deposit.name -> {
                sender_tv.text = snapshot.sender
                receiver_title.text = getString(R.string.transaction_hash)
                receiver_tv.text = snapshot.transactionHash
            }
            snapshot.type == SnapshotType.transfer.name -> {
                if (isPositive) {
                    sender_tv.text = snapshot.opponentFullName
                    receiver_tv.text = Session.getAccount()!!.full_name
                } else {
                    sender_tv.text = Session.getAccount()!!.full_name
                    receiver_tv.text = snapshot.opponentFullName
                }
            }
            else -> {
                sender_title.text = getString(R.string.transaction_hash)
                sender_tv.text = snapshot.transactionHash
                receiver_tv.text = snapshot.receiver
            }
        }
    }

    private fun getSnapshotType(type: String): String {
        val s = when (type) {
            "transfer" -> R.string.transfer
            "deposit" -> R.string.wallet_bottom_deposit
            "withdrawal" -> R.string.withdrawal
            "fee" -> R.string.fee
            "rebate" -> R.string.rebate
            else -> throw IllegalArgumentException("error snapshot type")
        }
        return requireContext().getString(s)
    }
}
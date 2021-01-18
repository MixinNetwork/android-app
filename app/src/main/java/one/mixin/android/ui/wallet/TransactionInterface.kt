package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentTransactionBinding
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigateUp
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.SnapshotType
import one.mixin.android.widget.DebugClickListener
import org.jetbrains.anko.textColorResource
import java.math.BigDecimal

interface TransactionInterface {
    fun initView(
        fragment: Fragment,
        contentBinding: FragmentTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        assetId: String?,
        snapshotId: String?,
        assetItem: AssetItem?,
        snapshotItem: SnapshotItem?
    ) {
        contentBinding.titleView.rightAnimator.visibility = View.GONE
        if (snapshotItem == null || assetItem == null) {
            if (snapshotId != null && assetId != null) {
                lifecycleScope.launch {
                    val asset = walletViewModel.simpleAssetItem(assetId)
                    val snapshot = walletViewModel.snapshotLocal(assetId, snapshotId)
                    if (asset == null || snapshot == null) {
                        fragment.context?.toast(R.string.error_data)
                    } else {
                        contentBinding.avatar.setOnClickListener {
                            clickAvatar(fragment, asset)
                        }
                        updateUI(fragment, contentBinding, asset, snapshot)
                        fetchThatTimePrice(fragment, lifecycleScope, walletViewModel, contentBinding, asset.assetId, snapshot)
                        refreshNoTransactionHashWithdrawal(fragment, contentBinding, lifecycleScope, walletViewModel, snapshot, asset)
                    }
                }
            } else {
                fragment.toast(R.string.error_data)
            }
        } else {
            contentBinding.avatar.setOnClickListener {
                clickAvatar(fragment, assetItem)
            }
            contentBinding.transactionIdTitleTv.setOnClickListener(object : DebugClickListener() {
                override fun onDebugClick() {
                    contentBinding.traceLl.visibility = View.VISIBLE
                }

                override fun onSingleClick() {
                }
            })
            updateUI(fragment, contentBinding, assetItem, snapshotItem)
            fetchThatTimePrice(fragment, lifecycleScope, walletViewModel, contentBinding, assetItem.assetId, snapshotItem)
            refreshNoTransactionHashWithdrawal(fragment, contentBinding, lifecycleScope, walletViewModel, snapshotItem, assetItem)
        }
    }

    private fun clickAvatar(fragment: Fragment, asset: AssetItem) {
        val curActivity = fragment.requireActivity()
        if (curActivity is WalletActivity) {
            fragment.view?.navigateUp()
        } else {
            WalletActivity.show(curActivity, asset, false)
        }
    }

    private fun fetchThatTimePrice(
        fragment: Fragment,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        contentBinding: FragmentTransactionBinding,
        assetId: String,
        snapshot: SnapshotItem,
    ) = lifecycleScope.launch {
        if (!fragment.isAdded) return@launch

        contentBinding.thatVa.displayedChild = POS_PB
        handleMixinResponse(
            invokeNetwork = { walletViewModel.ticker(assetId, snapshot.createdAt) },
            switchContext = Dispatchers.IO,
            successBlock = {
                val ticker = it.data
                if (ticker != null) {
                    contentBinding.thatVa.displayedChild = POS_TEXT
                    contentBinding.thatTv.apply {
                        text = if (ticker.priceUsd == "0") {
                            fragment.getString(R.string.wallet_transaction_that_time_no_value)
                        } else {
                            val amount = (BigDecimal(snapshot.amount).abs() * ticker.priceFiat()).priceFormat()
                            fragment.getString(R.string.wallet_transaction_that_time_value, "${Fiats.getSymbol()}$amount")
                        }
                        fragment.context?.let { c ->
                            setTextColor(c.colorFromAttribute(R.attr.text_minor))
                            setOnClickListener {
                                if (!fragment.isAdded) return@setOnClickListener

                                val balloon = createBalloon(c) {
                                    setArrowSize(10)
                                    setHeight(45)
                                    setCornerRadius(4f)
                                    setAlpha(0.9f)
                                    setAutoDismissDuration(3000L)
                                    setBalloonAnimation(BalloonAnimation.FADE)
                                    setText(fragment.getString(R.string.wallet_transaction_that_time_value_tip))
                                    setTextColorResource(R.color.white)
                                    setPaddingLeft(10)
                                    setPaddingRight(10)
                                    setBackgroundColorResource(R.color.colorLightBlue)
                                    setLifecycleOwner(fragment.viewLifecycleOwner)
                                }
                                balloon.show(this)
                            }
                        }
                    }
                } else {
                    showRetry(fragment, lifecycleScope, walletViewModel, contentBinding, assetId, snapshot)
                }
            },
            exceptionBlock = {
                showRetry(fragment, lifecycleScope, walletViewModel, contentBinding, assetId, snapshot)
                return@handleMixinResponse false
            },
            failureBlock = {
                showRetry(fragment, lifecycleScope, walletViewModel, contentBinding, assetId, snapshot)
                return@handleMixinResponse false
            }
        )
    }

    private fun showRetry(
        fragment: Fragment,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        contentBinding: FragmentTransactionBinding,
        assetId: String,
        snapshot: SnapshotItem,
    ) {
        contentBinding.apply {
            thatVa.displayedChild = POS_TEXT
            thatTv.apply {
                text = fragment.getString(R.string.click_retry)
                setTextColor(fragment.resources.getColor(R.color.colorDarkBlue, null))
                setOnClickListener { fetchThatTimePrice(fragment, lifecycleScope, walletViewModel, contentBinding, assetId, snapshot) }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(fragment: Fragment, contentBinding: FragmentTransactionBinding, asset: AssetItem, snapshot: SnapshotItem) {
        if (!fragment.isAdded) return

        contentBinding.apply {
            val amountVal = snapshot.amount.toFloatOrNull()
            val isPositive = if (amountVal == null) false else amountVal > 0
            ViewBadgeCircleImageBinding.bind(contentBinding.avatar).apply {
                bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
                badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            }
            valueTv.text = if (isPositive) "+${snapshot.amount.numberFormat()}"
            else snapshot.amount.numberFormat()
            symbolTv.text = asset.symbol
            valueTv.textColorResource = when {
                snapshot.type == SnapshotType.pending.name -> {
                    R.color.wallet_text_gray
                }
                isPositive -> {
                    R.color.wallet_green
                }
                else -> {
                    R.color.wallet_pink
                }
            }
            val amount = (BigDecimal(snapshot.amount).abs() * asset.priceFiat()).priceFormat()
            valueAsTv.text = fragment.getString(R.string.wallet_transaction_current_value, "${Fiats.getSymbol()}$amount")
            transactionIdTv.text = snapshot.snapshotId
            transactionTypeTv.text = getSnapshotType(fragment, snapshot.type)
            memoTv.text = snapshot.memo
            dateTv.text = snapshot.createdAt.fullDate()
            when (snapshot.type) {
                SnapshotType.deposit.name -> {
                    senderTitle.text = fragment.getString(R.string.sender)
                    senderTv.text = snapshot.sender
                    receiverTitle.text = fragment.getString(R.string.transaction_hash)
                    receiverTv.text = snapshot.transactionHash
                }
                SnapshotType.pending.name -> {
                    senderTitle.text = fragment.getString(R.string.sender)
                    senderTv.text = snapshot.sender
                    receiverTitle.text = fragment.getString(R.string.transaction_hash)
                    receiverTv.text = snapshot.transactionHash
                    transactionStatus.isVisible = true
                    transactionStatusTv.text =
                        fragment.getString(R.string.pending_confirmations, snapshot.confirmations, snapshot.assetConfirmations)
                }
                SnapshotType.transfer.name -> {
                    traceTv.text = snapshot.traceId
                    if (isPositive) {
                        senderTv.text = snapshot.opponentFullName
                        receiverTv.text = Session.getAccount()!!.fullName
                    } else {
                        senderTv.text = Session.getAccount()!!.fullName
                        receiverTv.text = snapshot.opponentFullName
                    }
                }
                else -> {
                    if (!asset.tag.isNullOrEmpty()) {
                        receiverTitle.text = fragment.getString(R.string.address)
                    } else {
                        receiverTitle.text = fragment.getString(R.string.receiver)
                    }
                    senderTitle.text = fragment.getString(R.string.transaction_hash)
                    senderTv.text = snapshot.transactionHash
                    receiverTv.text = snapshot.receiver
                }
            }
        }
    }

    private fun refreshNoTransactionHashWithdrawal(
        fragment: Fragment,
        contentBinding: FragmentTransactionBinding,
        lifecycleScope: CoroutineScope,
        walletViewModel: WalletViewModel,
        snapshot: SnapshotItem,
        asset: AssetItem,
    ) {
        if (snapshot.type == SnapshotType.withdrawal.name && snapshot.transactionHash.isNullOrBlank()) {
            lifecycleScope.launch {
                walletViewModel.refreshSnapshot(snapshot.snapshotId)?.let {
                    updateUI(fragment, contentBinding, asset, snapshot)
                }
            }
        }
    }

    fun getSnapshotType(fragment: Fragment, type: String): String {
        val s = when (type) {
            SnapshotType.transfer.name -> R.string.transfer
            SnapshotType.deposit.name, SnapshotType.pending.name -> R.string.wallet_bottom_deposit
            SnapshotType.withdrawal.name -> R.string.withdrawal
            SnapshotType.fee.name -> R.string.fee
            SnapshotType.rebate.name -> R.string.rebate
            SnapshotType.raw.name -> R.string.filters_raw
            else -> R.string.not_any
        }
        return fragment.requireContext().getString(s)
    }

    companion object {
        const val POS_PB = 0
        const val POS_TEXT = 1
    }
}

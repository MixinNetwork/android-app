package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_withdrawal_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.extension.formatPublicKey
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.toast
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment.Companion.POS_PB
import one.mixin.android.ui.conversation.tansfer.TransferBottomSheetDialogFragment.Companion.POS_PIN
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.math.BigDecimal
import java.util.UUID

class WithdrawalBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "WithdrawalBottomSheetDialogFragment"

        const val ARGS_WITHDRAWAL_ITEM = "args_withdrawal_item"
        const val ARGS_ASSET = "args_asset"

        fun newInstance(withdrawalItem: WithdrawalItem, asset: AssetItem) = WithdrawalBottomSheetDialogFragment().apply {
            val bundle = Bundle().apply {
                putParcelable(ARGS_WITHDRAWAL_ITEM, withdrawalItem)
                putParcelable(ARGS_ASSET, asset)
            }
            arguments = bundle
        }
    }

    private val withdrawalItem: WithdrawalItem by lazy {
        arguments!!.getParcelable<WithdrawalItem>(ARGS_WITHDRAWAL_ITEM)
    }

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_withdrawal_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.left_ib.setOnClickListener { dialog?.dismiss() }
        contentView.title_view.right_animator.setOnClickListener {
            if (contentView.addr_tv.visibility == GONE) {
                contentView.addr_tv.visibility = VISIBLE
                contentView.title_view.right_ib.setImageResource(R.drawable.ic_arrow_up)
            } else {
                contentView.addr_tv.visibility = GONE
                contentView.title_view.right_ib.setImageResource(R.drawable.ic_arrow_down)
            }
        }
        contentView.title_view.setSubTitle(getString(R.string.withdrawal_to, withdrawalItem.label),
            withdrawalItem.publicKey.formatPublicKey())
        contentView.addr_tv.text = withdrawalItem.publicKey
        if (!TextUtils.isEmpty(withdrawalItem.memo)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = withdrawalItem.memo
        }
        contentView.asset_icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        doAsync {
            val a = bottomViewModel.simpleAssetItem(asset.assetId)
            uiThread { a?.let { contentView.asset_icon.badge.loadImage(it.chainIconUrl, R.drawable.ic_avatar_place_holder) } }
        }
        contentView.balance.text = withdrawalItem.amount.numberFormat() + " " + asset.symbol
        contentView.balance_as.text = getString(R.string.wallet_unit_usd,
            "≈ ${(BigDecimal(withdrawalItem.amount) * BigDecimal(asset.priceUsd)).numberFormat2()}")
        contentView.keyboard.setKeyboardKeys(KEYS)
        contentView.keyboard.setOnClickKeyboardListener(object : Keyboard.OnClickKeyboardListener {
            override fun onKeyClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.delete()
                } else {
                    contentView.pin.append(value)
                }
            }

            override fun onLongClick(position: Int, value: String) {
                context?.vibrate(longArrayOf(0, 30))
                if (position == 11) {
                    contentView.pin.clear()
                } else {
                    contentView.pin.append(value)
                }
            }
        })
        contentView.pin.setListener(object : PinView.OnPinListener {
            override fun onUpdate(index: Int) {
                if (index == contentView.pin.getCount()) {
                    contentView.pin_va.displayedChild = POS_PB
                    bottomViewModel.withdrawal(withdrawalItem.addressId, withdrawalItem.amount,
                        contentView.pin.code(), UUID.randomUUID().toString(), withdrawalItem.memo)
                        .autoDisposable(scopeProvider).subscribe({
                        if (it.isSuccess) {
                            context?.updatePinCheck()
                            context?.toast(R.string.withdrawal_success)
                            callback?.onSuccess()
                            bottomViewModel.insertSnapshot(it.data!!)
                            dismiss()
                        } else {
                            contentView.pin.clear()
                            contentView.pin_va.displayedChild = POS_PIN
                            ErrorHandler.handleMixinError(it.errorCode)
                        }
                    }, {
                        ErrorHandler.handleError(it)
                        contentView.pin.clear()
                        contentView.pin_va.displayedChild = POS_PIN
                    })
                }
            }
        })
    }

    @SuppressLint("ParcelCreator")
    @Parcelize
    class WithdrawalItem(
        val publicKey: String,
        val amount: String,
        val memo: String? = null,
        val addressId: String,
        val label: String
    ) : Parcelable

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun onSuccess()
    }
}
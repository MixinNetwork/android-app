package one.mixin.android.ui.conversation.tansfer

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.VISIBLE
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.synthetic.main.fragment_transfer_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.Constants.BIOMETRIC_PIN_CHECK
import one.mixin.android.Constants.KEYS
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.putLong
import one.mixin.android.extension.updatePinCheck
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Asset
import one.mixin.android.vo.User
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.Keyboard
import one.mixin.android.widget.PinView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.math.BigDecimal

class TransferBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "TransferBottomSheetDialogFragment"

        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_MEMO = "args_memo"
        const val ARGS_TRACE = "args_trace"
        const val ARGS_PIN = "args_pin"

        const val POS_PIN = 0
        const val POS_PB = 1

        fun newInstance(
            user: User,
            transferAmount: String,
            asset: Asset,
            trace: String?,
            transferMemo: String?,
            pin: String? = null
        ) =
            TransferBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_USER, user)
                putString(ARGS_AMOUNT, transferAmount)
                putString(ARGS_MEMO, transferMemo)
                putParcelable(ARGS_ASSET, asset)
                putString(ARGS_TRACE, trace)
                pin?.let { putString(ARGS_PIN, pin) }
            }
    }

    private val user: User by lazy {
        arguments!!.getParcelable(ARGS_USER) as User
    }

    private val amount: String by lazy {
        arguments!!.getString(ARGS_AMOUNT)
    }

    private val memo: String? by lazy {
        arguments!!.getString(ARGS_MEMO)
    }

    private val asset: Asset by lazy {
        arguments!!.getParcelable<Asset>(ARGS_ASSET)
    }

    private val trace: String? by lazy {
        arguments!!.getString(ARGS_TRACE)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        if (!TextUtils.isEmpty(memo)) {
            contentView.memo.visibility = VISIBLE
            contentView.memo.text = memo
        }
        contentView.asset_icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        doAsync {
            val a = bottomViewModel.simpleAssetItem(asset.assetId)
            uiThread { a?.let { contentView.asset_icon.badge.loadImage(it.chainIconUrl, R.drawable.ic_avatar_place_holder) } }
        }
        contentView.balance.text = amount.numberFormat() + " " + asset.symbol
        contentView.balance_as.text = getString(R.string.wallet_unit_usd,
        "≈ ${(BigDecimal(amount) * BigDecimal(asset.priceUsd)).numberFormat2()}")
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
                    bottomViewModel.transfer(asset.assetId, user.userId, amount,
                        contentView.pin.code(), trace, memo).autoDisposable(scopeProvider)
                        .subscribe({
                            contentView.pin_va.displayedChild = POS_PIN
                            if (it.isSuccess) {
                                defaultSharedPreferences.putLong(BIOMETRIC_PIN_CHECK, System.currentTimeMillis())
                                context?.updatePinCheck()
                                dismiss()
                                callback?.onSuccess()
                            } else {
                                contentView.pin.clear()
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
        val pin = arguments!!.getString(ARGS_PIN)
        if (pin != null) {
            contentView.pin.set(pin)
        }
    }

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun onSuccess()
    }
}
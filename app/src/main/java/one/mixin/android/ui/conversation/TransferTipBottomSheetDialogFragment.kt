package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_transfer_tip_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

class TransferTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "TransferTipBottomSheetDialogFragment"
        private const val ARGS_NAME = "name"
        private const val ARGS_AMOUNT = "amount"

        fun newInstance(name: String?, assetItem: AssetItem, amount: Double) =
            TransferTipBottomSheetDialogFragment().withArgs {
                putParcelable(ARGS_ASSET, assetItem)
                putDouble(ARGS_AMOUNT, amount)
                putString(ARGS_NAME, name)
            }
    }

    private var mCountDownTimer: CountDownTimer? = null

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)!!
    }

    private val amount: Double by lazy {
        arguments!!.getDouble(ARGS_AMOUNT)
    }

    private val name: String? by lazy {
        arguments!!.getString(ARGS_NAME)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_transfer_tip_bottom_sheet, null)
        contentView.asset_icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
        lifecycleScope.launch(Dispatchers.IO) {
            if (!isAdded) return@launch

            bottomViewModel.simpleAssetItem(asset.assetId)?.let {
                withContext(Dispatchers.Main) {
                    contentView.asset_icon.badge.loadImage(
                        it.chainIconUrl,
                        R.drawable.ic_avatar_place_holder
                    )
                }
            }
        }
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.warning_tv.text =
            getString(R.string.wallet_transaction_tip, name, amount, asset.symbol)
        contentView.continue_tv.setOnClickListener {
            callback?.onSuccess()
            dismiss()
        }
        contentView.cancel_tv.setOnClickListener { dismiss() }

        startCountDown()
    }

    override fun dismiss() {
        mCountDownTimer?.cancel()
        super.dismiss()
    }

    private fun startCountDown() {
        mCountDownTimer?.cancel()
        mCountDownTimer = object : CountDownTimer(4000, 1000) {

            override fun onTick(l: Long) {
                if (isAdded) {
                    contentView.continue_tv.text =
                        getString(R.string.wallet_transaction_continue_count, l / 1000)
                }
            }

            override fun onFinish() {
                if (isAdded) {
                    contentView.continue_tv.text = getString(R.string.wallet_transaction_continue)
                    contentView.cancel_tv.setTextColor(
                        resources.getColor(
                            R.color.colorDarkBlue,
                            null
                        )
                    )
                    contentView.continue_tv.isEnabled = true
                }
            }
        }
        mCountDownTimer?.start()
    }

    var callback: Callback? = null

    interface Callback {
        fun onSuccess()
    }
}

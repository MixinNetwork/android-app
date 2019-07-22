package one.mixin.android.ui.conversation

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import kotlinx.android.synthetic.main.fragment_withdrawal_tip_bottom_sheet.view.*
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.BottomSheet

class WithdrawalTipBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "WithdrawalTipBottomSheetDialogFragment"

        fun newInstance(assetItem: AssetItem) = WithdrawalTipBottomSheetDialogFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
        }
    }

    private var mCountDownTimer: CountDownTimer? = null

    private val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_withdrawal_tip_bottom_sheet, null)
        (dialog as BottomSheet).run {
            setCustomView(contentView)
            dismissClickOutside = false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_tv.text = getString(R.string.bottom_withdrawal_title, asset.symbol)
        contentView.continue_tv.setOnClickListener {
            callback?.onSuccess()
            dismiss()
        }
        contentView.change_tv.setOnClickListener { dismiss() }

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
                    contentView.change_tv.text = getString(R.string.bottom_withdrawal_change_amount_count, l / 1000)
                }
            }

            override fun onFinish() {
                if (isAdded) {
                    contentView.change_tv.text = getString(R.string.bottom_withdrawal_change_amount)
                    contentView.continue_tv.setTextColor(resources.getColor(R.color.colorDarkBlue, null))
                    contentView.change_tv.isEnabled = true
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

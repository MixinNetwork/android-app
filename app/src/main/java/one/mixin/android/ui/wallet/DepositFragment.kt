package one.mixin.android.ui.wallet

import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_deposit_key.*
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

abstract class DepositFragment : Fragment() {

    protected val asset: AssetItem by lazy {
        arguments!!.getParcelable<AssetItem>(ARGS_ASSET)
    }

    override fun onStop() {
        super.onStop()
        title.removeCallbacks(showTipRunnable)
    }

    protected fun showTip() {
        val depositChainSet = defaultSharedPreferences.getStringSet(Constants.Account.PREF_SHOW_DEPOSIT_TIP_CHAIN_SET, null)
        val showTip = if (depositChainSet == null) {
            true
        } else {
            !depositChainSet.contains(asset.chainId)
        }
        if (!showTip) return
        title.postDelayed(showTipRunnable, 3000)
    }

    private val showTipRunnable = Runnable {
        DepositTipBottomSheetDialogFragment.newInstance(asset)
            .showNow(parentFragmentManager, DepositTipBottomSheetDialogFragment.TAG)
    }
}

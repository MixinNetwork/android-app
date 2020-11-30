package one.mixin.android.ui.wallet

import androidx.fragment.app.Fragment
import one.mixin.android.databinding.ViewTitleBinding
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

abstract class DepositFragment : Fragment() {

    protected var _titleBinding: ViewTitleBinding? = null
    protected val titleBinding get() = requireNotNull(_titleBinding)

    val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }

    override fun onStop() {
        super.onStop()
        titleBinding.root.removeCallbacks(showTipRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _titleBinding = null
    }

    protected fun showTip() {
        titleBinding.root.post(showTipRunnable)
    }

    private val showTipRunnable = Runnable {
        DepositTipBottomSheetDialogFragment.newInstance(asset)
            .showNow(parentFragmentManager, DepositTipBottomSheetDialogFragment.TAG)
    }
}

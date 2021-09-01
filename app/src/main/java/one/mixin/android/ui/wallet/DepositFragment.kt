package one.mixin.android.ui.wallet

import androidx.fragment.app.Fragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

abstract class DepositFragment : Fragment() {

    val asset: AssetItem by lazy {
        requireArguments().getParcelable(ARGS_ASSET)!!
    }
}

package one.mixin.android.ui.home.web3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.FragmentPolygonBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.WalletCreateBottomSheetDialogFragment

class PolygonFragment : BaseFragment() {
    companion object {
        const val TAG = "PolygonFragment"
    }

    private var _binding: FragmentPolygonBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPolygonBinding.inflate(inflater, container, false)
        binding.chainCard.setContent("Polygon Account", "Access dapps and DeFi projects.", R.drawable.ic_polygon)
        binding.chainCard.setOnCreateListener {
            WalletCreateBottomSheetDialogFragment.newInstance(WalletCreateBottomSheetDialogFragment.TYPE_POLYGON).showNow(parentFragmentManager, WalletConnectBottomSheetDialogFragment.TAG)
        }
        return binding.root
    }

}
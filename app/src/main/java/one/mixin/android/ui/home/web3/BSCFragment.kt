package one.mixin.android.ui.home.web3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBscBinding
import one.mixin.android.ui.common.BaseFragment

class BSCFragment : BaseFragment() {
    companion object {
        const val TAG = "BSCFragment"
    }
    private var _binding: FragmentBscBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBscBinding.inflate(inflater, container, false)
        binding.chainCard.setContent("BSC Account", "Access dapps and DeFi projects.", R.drawable.ic_bsc)
        return binding.root
    }

}
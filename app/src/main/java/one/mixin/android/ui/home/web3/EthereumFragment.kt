package one.mixin.android.ui.home.web3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import one.mixin.android.databinding.FragmentEthereumBinding
import one.mixin.android.ui.common.BaseFragment

class EthereumFragment : BaseFragment() {
    companion object {
        const val TAG = "EthereumFragment"
    }

    private var _binding: FragmentEthereumBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEthereumBinding.inflate(inflater, container, false)
        binding.chainCard.setContent("Ethereum Account", "Access dapps and DeFi projects.", R.drawable.ic_ethereum)
        return binding.root
    }

}
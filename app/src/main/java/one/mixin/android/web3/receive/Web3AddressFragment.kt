package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.InputAmountBottomSheetDialogFragment
import one.mixin.android.databinding.FragmentWeb3AddressBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.DepositShareActivity
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.util.getChainName

@AndroidEntryPoint
class Web3AddressFragment : BaseFragment() {
    companion object {
        const val TAG = "Web3ReceiveFragment"

        fun newInstance(web3Token: Web3TokenItem, address: String): Web3AddressFragment {
            val fragment = Web3AddressFragment()
            val args = Bundle().apply {
                putParcelable("web3_token", web3Token)
                putString("address", address)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private var _binding: FragmentWeb3AddressBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var address: String
    private lateinit var web3Token: Web3TokenItem

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        address = arguments?.getString("address") ?: ""
        web3Token = arguments?.getParcelable("web3_token") ?: throw IllegalArgumentException("web3Token is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWeb3AddressBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        binding.root.setOnClickListener { }
        binding.title.setOnClickListener { }
        binding.title.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        lifecycleScope.launch {
            val wallet = walletViewModel.getWalletByDestination(address)
            if (wallet != null) {
                binding.title.setSubTitle(getString(R.string.Receive), wallet.name)
            }
            binding.copy.setOnClickListener {
                context?.heavyClickVibrate()
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                toast(R.string.copied_to_clipboard)
            }
            binding.amount.setOnClickListener {
                InputAmountBottomSheetDialogFragment.newInstance(
                    web3Token,
                    address
                ).apply {
                    this.onCopyClick = { depositUri ->
                        this@Web3AddressFragment.lifecycleScope.launch {
                            context?.heavyClickVibrate()
                            context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, depositUri))
                            toast(R.string.copied_to_clipboard)
                        }
                    }
                    this.onShareClick = { amount, depositUri ->
                        this@Web3AddressFragment.lifecycleScope.launch {
                            DepositShareActivity.show(
                                requireContext(),
                                web3Token,
                                address,
                                depositUri,
                                amount
                            )
                        }
                    }
                }.show(parentFragmentManager, InputAmountBottomSheetDialogFragment.TAG)
            }

            binding.share.setOnClickListener {
                refreshScreenshot(requireContext(), 0x66FF0000)
                DepositShareActivity.show(
                    requireContext(),
                    web3Token,
                    address,
                    address
                )
            }

            binding.addressTitle.setText(R.string.Address)
            val isSolana = !address.startsWith("0x")
            binding.addressView.loadAddress(
                scopeProvider,
                address,
                if(isSolana) R.drawable.ic_web3_logo_sol else R.drawable.ic_web3_logo_eth,
                ""
            )
            binding.assetName.text = web3Token.name
            binding.networkName.text = getChainName(web3Token.chainId, web3Token.chainName, web3Token.assetKey)
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

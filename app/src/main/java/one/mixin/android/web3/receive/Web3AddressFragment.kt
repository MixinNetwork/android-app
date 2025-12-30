package one.mixin.android.web3.receive

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.AssetId.USDC_ASSET_BASE_ID
import one.mixin.android.Constants.AssetId.USDC_ASSET_BEP_ID
import one.mixin.android.Constants.AssetId.USDC_ASSET_ETH_ID
import one.mixin.android.Constants.AssetId.USDC_ASSET_POL_ID
import one.mixin.android.Constants.AssetId.USDC_ASSET_SOL_ID
import one.mixin.android.Constants.AssetId.USDT_ASSET_BEP_ID
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.Constants.AssetId.USDT_ASSET_POL_ID
import one.mixin.android.Constants.AssetId.USDT_ASSET_SOL_ID
import one.mixin.android.R
import one.mixin.android.compose.InputAmountBottomSheetDialogFragment
import one.mixin.android.databinding.FragmentWeb3AddressBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.DepositChooseNetworkBottomSheetDialogFragment
import one.mixin.android.ui.wallet.DepositFragment
import one.mixin.android.ui.wallet.DepositShareActivity
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.util.getChainNetwork
import one.mixin.android.web3.js.Web3Signer

@AndroidEntryPoint
class Web3AddressFragment : BaseFragment() {
    companion object {
        const val TAG = "Web3ReceiveFragment"
        private const val ARGS_HIDE_NETWORK_SWITCH = "args_hide_network_switch"

        fun newInstance(web3Token: Web3TokenItem, address: String?, hideNetworkSwitch: Boolean = false): Web3AddressFragment {
            val fragment = Web3AddressFragment()
            val args = Bundle().apply {
                putParcelable("web3_token", web3Token)
                putString("address", address)
                putBoolean(ARGS_HIDE_NETWORK_SWITCH, hideNetworkSwitch)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private val walletViewModel by viewModels<WalletViewModel>()
    private var _binding: FragmentWeb3AddressBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var address: String? = null
    private lateinit var web3Token: Web3TokenItem
    private var hideNetworkSwitch: Boolean = false
    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        address = arguments?.getString("address")
        web3Token = arguments?.getParcelableCompat("web3_token", Web3TokenItem::class.java) ?: throw IllegalArgumentException("web3Token is required")
        hideNetworkSwitch = arguments?.getBoolean(ARGS_HIDE_NETWORK_SWITCH, false) ?: false
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
        binding.title.rightIb.setOnClickListener {
            requireContext().openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
        }
        lifecycleScope.launch {
            val address = this@Web3AddressFragment.address
            if (address == null) {
                binding.va.displayedChild = 1
                return@launch
            }
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

            updateUI()
            if (!hideNetworkSwitch) {
                if (Constants.AssetId.usdtAssets.containsKey(web3Token.assetId)) {
                    binding.networkChipGroup.isVisible = true
                    initChips(
                        if (Web3Signer.evmAddress.isBlank()) {
                            mapOf(
                                USDT_ASSET_SOL_ID to "Solana",
                                )
                        } else if (Web3Signer.solanaAddress.isBlank()) {
                            mapOf(
                                USDT_ASSET_ETH_ID to "Ethereum",
                                USDT_ASSET_POL_ID to "Polygon",
                                USDT_ASSET_BEP_ID to "BSC",
                            )

                        } else {
                            mapOf(
                                USDT_ASSET_ETH_ID to "Ethereum",
                                USDT_ASSET_SOL_ID to "Solana",
                                USDT_ASSET_POL_ID to "Polygon",
                                USDT_ASSET_BEP_ID to "BSC",
                            )
                        }
                    )
                } else if (Constants.AssetId.usdcAssets.containsKey(web3Token.assetId)) {
                    initChips(
                        if (Web3Signer.evmAddress.isBlank()) {
                            mapOf(
                                USDC_ASSET_SOL_ID to "Solana",
                            )
                        } else if (Web3Signer.solanaAddress.isBlank()) {
                            mapOf(
                                USDC_ASSET_ETH_ID to "Ethereum",
                                USDC_ASSET_BASE_ID to "Base",
                                USDC_ASSET_POL_ID to "Polygon",
                                USDC_ASSET_BEP_ID to "BSC"
                            )
                        } else {
                            Constants.AssetId.usdcAssets
                        }
                    )
                } else if (Constants.AssetId.ethAssets.containsKey(web3Token.assetId)) {
                    initChips(Constants.AssetId.ethAssets)
                }
            }
        }
        lifecycleScope.launch {
            val name = walletViewModel.findChainById(web3Token.chainId)?.name
                ?: getChainNetwork(web3Token.assetId, web3Token.chainId, web3Token.assetKey)
            showDepositChooseNetworkBottomSheetDialog(web3Token, name)
        }

        return binding.root
    }

    private fun initChips(map:Map<String,String>) {
        binding.apply {
            networkChipGroup.isVisible = true
            networkChipGroup.isSingleSelection = true
            networkChipGroup.removeAllViews()
            map.entries.forEach { entry ->
                val chip = Chip(requireContext()).apply {
                    tag = entry.key
                    isChecked = entry.key == web3Token.assetId
                    text = entry.value
                    isClickable = true
                    setOnClickListener {
                        selectToken(entry.key)
                    }
                }
                networkChipGroup.addView(chip)
            }
        }
        updateChips()
    }

    private fun selectToken(id:String) {
        lifecycleScope.launch {
            walletViewModel.getTokenByWalletAndAssetId(Web3Signer.currentWalletId,id)?.let {
                web3Token = it
            }
            if (web3Token.isSolanaChain()) {
                address = Web3Signer.solanaAddress
            } else {
                address = Web3Signer.evmAddress
            }
            updateUI()
            updateChips()
        }
    }

    private fun updateUI() {
        val address = this@Web3AddressFragment.address ?: return
        binding.addressView.loadAddress(
            scopeProvider,
            address,
            web3Token,
            ""
        )
        binding.assetName.text = "${web3Token.name} (${web3Token.symbol})"

        binding.addressDesc.isVisible = true
        lifecycleScope.launch {
            val chain = walletViewModel.findChainById(web3Token.chainId)
            binding.addressDesc.text = getTipsByAsset(web3Token, chain)
            binding.networkName.text = walletViewModel.findChainById(web3Token.chainId)?.name
                ?: getChainNetwork(web3Token.assetId, web3Token.chainId, web3Token.assetKey)
        }
    }

    private fun updateChips() {
        binding.networkChipGroup.children.forEach {
            if (it is Chip) {
                if (it.tag == web3Token.assetId) {
                    val accentColor = requireContext().colorFromAttribute(R.attr.color_accent)
                    it.setTextColor(accentColor)
                    it.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    it.chipStrokeColor = ColorStateList.valueOf(accentColor)
                    it.chipStrokeWidth = 1.dp.toFloat()
                } else {
                    it.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    it.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    it.chipStrokeColor = ColorStateList.valueOf(requireContext().colorFromAttribute(R.attr.bg_window))
                    it.chipStrokeWidth = 1.dp.toFloat()
                }
            }
        }
    }

    private var showed = false
    private fun showDepositChooseNetworkBottomSheetDialog(
        asset: Web3TokenItem,
        name: String?,
    ) {
        if (showed) return
        showed = true // run only once
        lifecycleScope.launch {
            DepositChooseNetworkBottomSheetDialogFragment.newInstance(asset = asset.toTokenItem(), name)
                .showNow(childFragmentManager, DepositFragment.TAG)
        }
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

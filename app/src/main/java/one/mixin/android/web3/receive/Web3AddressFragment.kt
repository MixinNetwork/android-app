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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.InputAmountBottomSheetDialogFragment
import one.mixin.android.databinding.FragmentWeb3AddressBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.openUrl
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
    private var currentId: String? = null

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        address = arguments?.getString("address") ?: ""
        web3Token = arguments?.getParcelableCompat("web3_token", Web3TokenItem::class.java) ?: throw IllegalArgumentException("web3Token is required")
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
                web3Token.iconUrl ?:"",
                ""
            )
            binding.assetName.text = web3Token.symbol
            binding.networkName.text = getChainName(web3Token.chainId, web3Token.chainName, web3Token.assetKey)
            if (Constants.AssetId.ethAssets.containsKey(web3Token.assetId)) {
                binding.networkChipGroup.isVisible = true
                initChips()
            } else {
                binding.networkChipGroup.isVisible = false
            }
        }
        return binding.root
    }

    private fun initChips() {
        currentId = web3Token.assetId
        binding.apply {
            networkChipGroup.isSingleSelection = true
            networkChipGroup.removeAllViews()
            Constants.AssetId.ethAssets.entries.forEach { entry ->
                val chip = Chip(requireContext()).apply {
                    tag = entry.key
                    isChecked = entry.key == currentId
                    text = entry.value
                    isClickable = true
                    setOnClickListener {
                        currentId = entry.key
                        updateChips()
                    }
                }
                networkChipGroup.addView(chip)
            }
        }
        updateChips()
    }

    private fun updateChips() {
        binding.networkChipGroup.children.forEach {
            if (it is Chip) {
                if (it.tag == currentId) {
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

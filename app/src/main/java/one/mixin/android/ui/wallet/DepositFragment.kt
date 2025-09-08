package one.mixin.android.ui.wallet

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.AssetId.BYTOM_CLASSIC_ASSET_ID
import one.mixin.android.Constants.AssetId.MGD_ASSET_ID
import one.mixin.android.Constants.AssetId.OMNI_USDT_ASSET_ID
import one.mixin.android.R
import one.mixin.android.compose.InputAmountBottomSheetDialogFragment
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.verifyCurve25519Signature
import one.mixin.android.databinding.FragmentDepositBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.isNullOrEmpty
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.BlockConfirmationsBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.util.ErrorHandler.Companion.ADDRESS_GENERATING
import one.mixin.android.util.getChainName
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class DepositFragment : BaseFragment() {
    companion object {
        const val TAG = "DepositFragment"

        fun newInstance(token: TokenItem) = DepositFragment().withArgs {
            putParcelable(ARGS_ASSET, token)
        }
    }

    private val notSupportDepositAssets = arrayOf(OMNI_USDT_ASSET_ID, BYTOM_CLASSIC_ASSET_ID, MGD_ASSET_ID)

    private var _binding: FragmentDepositBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val walletViewModel by viewModels<WalletViewModel>()

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDepositBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val asset = requireNotNull(requireArguments().getParcelableCompat(ARGS_ASSET, TokenItem::class.java)) { "required TokenItem can not be null" }
        initView(asset)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initView(asset: TokenItem) {
        val notSupport = notSupportDepositAssets.any { it == asset.assetId }
        binding.apply {
            title.apply {
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightAnimator.setOnClickListener { context?.openUrl(getString(R.string.deposit_url)) }
            }
            title.setSubTitle(getString(R.string.Deposit_Token, asset.symbol), getString(R.string.Privacy_Wallet), R.drawable.ic_wallet_privacy)
            addressDesc.setText(getString(R.string.deposit_tip_common, asset.symbol))
            if (notSupport) {
                notSupportLl.isVisible = true
                sv.isVisible = false
                val symbol = if (asset.assetId == OMNI_USDT_ASSET_ID) "OMNI-USDT" else asset.symbol
                val info = getString(R.string.not_supported_deposit, symbol, symbol)
                val url = getString(R.string.not_supported_deposit_url)
                notSupportTv.highlightStarTag(info, arrayOf(url))
            } else {
                if (Constants.AssetId.usdtAssets.contains(asset.assetId)) {
                    networkChipGroup.isVisible = true
                    initChips(asset, Constants.AssetId.usdtAssets)
                } else if (Constants.AssetId.usdcAssets.contains(asset.assetId)) {
                    networkChipGroup.isVisible = true
                    initChips(asset, Constants.AssetId.usdcAssets)
                } else if (Constants.AssetId.ethAssets.contains(asset.assetId)) {
                    networkChipGroup.isVisible = true
                    initChips(asset, Constants.AssetId.ethAssets)
                } else if (Constants.AssetId.btcAssets.contains(asset.assetId)) {
                    networkChipGroup.isVisible = true
                    initChips(asset, Constants.AssetId.btcAssets)
                } else {
                    networkChipGroup.isVisible = false
                }

                notSupportLl.isVisible = false
                sv.isVisible = true
                binding.assetName.text = asset.name
                binding.networkName.text = getChainName(asset.chainId, asset.chainName, asset.assetKey)
                binding.minimumDepositValue.text = "${asset.dust} ${asset.symbol}"
                binding.blockConfirmationsValue.text = asset.confirmations.toString()
            }
        }

        if (!notSupport) {
            refreshDeposit(asset)
        }
    }

    private val localMap = mutableMapOf<String, DepositEntry>()

    private fun initChips(asset: TokenItem, uAssets: Map<String, String>) {
        binding.apply {
            networkChipGroup.isSingleSelection = true
            networkChipGroup.removeAllViews()
            uAssets.entries.forEach { entry ->
                val chip =
                    Chip(requireContext()).apply {
                        val c = this
                        text = entry.value
                        isClickable = true
                        c.tag = entry.key
                        val same = entry.key == asset.assetId
                        if (same) {
                            isChecked = true
                            val accentColor = requireContext().colorFromAttribute(R.attr.color_accent)
                            setTextColor(accentColor)
                            chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                            chipStrokeColor = ColorStateList.valueOf(accentColor)
                            chipStrokeWidth = 1.dp.toFloat()
                        } else {
                            isChecked = false
                            setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                            chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                            chipStrokeColor = ColorStateList.valueOf(requireContext().colorFromAttribute(R.attr.bg_window))
                            chipStrokeWidth = 1.dp.toFloat()
                        }
                        setOnClickListener {
                            if (same) return@setOnClickListener
                            syncJob?.cancel()
                            syncJob =
                                lifecycleScope.launch {
                                    showLoading()
                                    val newAsset = walletViewModel.findOrSyncAsset(entry.key)
                                    if (newAsset == null) {
                                        toast(R.string.Not_found)
                                    } else {
                                        initChips(newAsset, uAssets)
                                        val localDepositEntry = localMap[newAsset.assetId]
                                        if (localDepositEntry == null) {
                                            refreshDeposit(newAsset)
                                        } else {
                                            updateUI(newAsset, localDepositEntry)
                                            hideLoading()
                                        }
                                    }
                                }
                        }
                    }
                networkChipGroup.addView(chip)
            }
        }
    }

    private var syncJob: Job? = null

    private var showed = false

    private fun showDepositChooseNetworkBottomSheetDialog(
        asset: TokenItem,
        depositEntry: DepositEntry,
    ) {
        if (showed) return
        showed = true // run only once
        if (!notSupportDepositAssets.any { it == asset.assetId }) {
            lifecycleScope.launch {
                DepositChooseNetworkBottomSheetDialogFragment.newInstance(asset = asset)
                    .apply {
                        this.callback = {
                            val existTag = !depositEntry.tag.isNullOrBlank()
                            if (existTag) {
                                alertDialogBuilder()
                                    .setTitle(R.string.Notice)
                                    .setCancelable(false)
                                    .setMessage(
                                        getString(
                                            if (asset.assetId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                                                R.string.deposit_notice_tag
                                            } else if (asset.assetId == Constants.ChainId.EOS_CHAIN_ID) {
                                                R.string.deposit_notice_eos
                                            } else {
                                                R.string.deposit_notice
                                            },
                                            asset.symbol,
                                        ),
                                    )
                                    .setPositiveButton(R.string.OK) { dialog, _ ->
                                        dialog.dismiss()
                                    }.show()
                            }
                        }
                    }
                    .showNow(childFragmentManager, TAG)
            }
        }
    }

    private fun refreshDeposit(asset: TokenItem) {
        showLoading()
        lifecycleScope.launch {
            val (depositEntry, different, code) = walletViewModel.findAndCheckDepositEntry(asset.chainId, asset.assetId)
            if (depositEntry == null) {
                if (code == ADDRESS_GENERATING) {
                    binding.apply {
                        notSupportLl.isVisible = true
                        sv.isVisible = false
                        val symbol = asset.symbol
                        val info = getString(R.string.suspended_deposit, symbol, symbol)
                        notSupportTv.text = info
                        contactSupport.isVisible = true
                        contactSupport.setOnClickListener {
                            lifecycleScope.launch {
                                val userTeamMixin = walletViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                                if (userTeamMixin == null) {
                                    toast(R.string.Data_error)
                                } else {
                                    ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                                }
                            }
                        }
                    }
                } else {
                    delay(500)
                    refreshDeposit(asset)
                }
            } else {
                localMap[asset.assetId] = depositEntry
                showDepositChooseNetworkBottomSheetDialog(asset, depositEntry)
                if (different && asset.assetId != Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) {
                    AddressChangedBottomSheet.newInstance(asset).showNow(parentFragmentManager, AddressChangedBottomSheet.TAG)
                }
                updateUI(asset, depositEntry)
                hideLoading()
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            loading.isVisible = true
            bottom.isVisible = false
            addressView.isVisible = false
            addressTitle.isVisible = false
            addressDesc.isVisible = false
            tipLl.isVisible = false
            memoTitle.isVisible = false
            memoView.isVisible = false
        }
    }

    private fun hideLoading() {
        binding.apply {
            loading.isVisible = false
            addressView.isVisible = true
            addressTitle.isVisible = true
            addressDesc.isVisible = true
            tipLl.isVisible = true
        }
    }

    private fun updateUI(
        asset: TokenItem,
        depositEntry: DepositEntry,
    ) {
        if (viewDestroyed()) return

        val destination = depositEntry.destination
        val tag = depositEntry.tag
        val signature = depositEntry.signature.hexStringToByteArray()

        if (destination.isBlank() || signature.isNullOrEmpty()) return
        val pubs = Constants.SAFE_PUBLIC_KEY.map { it.hexStringToByteArray() }
        val message =
            if (tag.isNullOrBlank()) {
                destination
            } else {
                "$destination:$tag"
            }.toByteArray().sha3Sum256()
        val verify = pubs.any { pub -> verifyCurve25519Signature(message, signature, pub) }
        if (verify) {
            val noTag = tag.isNullOrBlank()

            // Check if asset supports amount input (QR code generation)
            val supportsAmountInput = when (asset.chainId) {
                Constants.ChainId.BITCOIN_CHAIN_ID,
                Constants.ChainId.ETHEREUM_CHAIN_ID,
                Constants.ChainId.Base,
                Constants.ChainId.Arbitrum,
                Constants.ChainId.Optimism,
                Constants.ChainId.BinanceSmartChain,
                Constants.ChainId.Polygon,
                Constants.ChainId.Litecoin,
                Constants.ChainId.Dogecoin,
                Constants.ChainId.Dash,
                Constants.ChainId.Monero,
                Constants.ChainId.Solana,
                    -> true

                else -> false
            }

            binding.apply {
                if (noTag) {
                    memoView.isVisible = false
                    memoTitle.isVisible = false
                } else {
                    memoView.isVisible = true
                    memoTitle.isVisible = true
                    if (asset.assetId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                        memoTitle.setText(R.string.Tag)
                    } else {
                        memoTitle.setText(R.string.Memo)
                    }
                    if (asset.assetId == Constants.ChainId.EOS_CHAIN_ID) {
                        addressTitle.setText(R.string.Account)
                    } else {
                        addressTitle.setText(R.string.Address)
                    }
                    memoView.setAsset(
                        parentFragmentManager,
                        scopeProvider,
                        asset,
                        depositEntry,
                        null,
                        true,
                        if (asset.assetId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                            getString(R.string.deposit_tag_notice)
                        } else {
                            getString(R.string.deposit_memo_notice)
                        },
                        hideCopy = noTag
                    )
                }
                addressView.setAsset(
                    parentFragmentManager,
                    scopeProvider,
                    asset,
                    depositEntry,
                    null,
                    false,
                    if (noTag) {
                        null
                    } else if (asset.assetId == Constants.ChainId.RIPPLE_CHAIN_ID) {
                        getString(R.string.deposit_notice_tag, asset.symbol)
                    } else {
                        getString(R.string.deposit_notice, asset.symbol)
                    },
                    hideCopy = noTag
                )

                binding.copy.setOnClickListener {
                    context?.heavyClickVibrate()
                    context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, depositEntry.destination))
                    toast(R.string.copied_to_clipboard)
                }

                // Only show amount button for supported chains
                if (supportsAmountInput) {
                    binding.amount.isVisible = true
                    binding.bottom.weightSum = 3f
                    binding.amount.setOnClickListener {
                        InputAmountBottomSheetDialogFragment.newInstance(
                            asset,
                            depositEntry.destination
                        ).apply {
                            this.onCopyClick = { address ->
                                this@DepositFragment.lifecycleScope.launch {
                                    context?.heavyClickVibrate()
                                    context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                                    toast(R.string.copied_to_clipboard)
                                }
                            }
                            this.onShareClick = { amount, address ->
                                this@DepositFragment.lifecycleScope.launch {
                                    DepositShareActivity.show(
                                        requireContext(),
                                        asset,
                                        depositEntry.destination,
                                        address,
                                        amount
                                    )
                                }
                            }
                        }.showNow(parentFragmentManager, InputAmountBottomSheetDialogFragment.TAG)
                    }
                } else {
                    binding.amount.isVisible = false
                    binding.bottom.weightSum = 2f
                }

                binding.share.setOnClickListener {
                    refreshScreenshot(requireContext(), 0x66FF0000)
                    DepositShareActivity.show(
                        requireContext(),
                        asset,
                        depositEntry.destination,
                    )
                }
                bottom.isVisible = noTag
            }
        } else {
            binding.apply {
                notSupportLl.isVisible = true
                sv.isVisible = false
                notSupportTv.setText(R.string.verification_failed)
                bottom.isVisible = false
            }

        }
        binding.networkChipGroup.children.forEach { clip ->
            (clip as? Chip)?.apply {
                val same = clip.tag == asset.assetId
                if (same) {
                    isChecked = true
                    val accentColor = requireContext().colorFromAttribute(R.attr.color_accent)
                    setTextColor(accentColor)
                    chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    chipStrokeColor = ColorStateList.valueOf(accentColor)
                    chipStrokeWidth = 1.dp.toFloat()
                } else {
                    isChecked = false
                    setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                    chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    chipStrokeColor = ColorStateList.valueOf(requireContext().colorFromAttribute(R.attr.bg_window))
                    chipStrokeWidth = 1.dp.toFloat()
                }
            }
        }
        binding.assetName.text = asset.symbol
        binding.networkName.text = getChainName(asset.chainId, asset.chainName, asset.assetKey)
        binding.minimumDepositValue.text = "${asset.dust} ${asset.symbol}"
        binding.blockConfirmationsValue.text = asset.confirmations.toString()
        binding.blockConfirmations.setOnClickListener {
            BlockConfirmationsBottomSheetDialogFragment.newInstance(asset.confirmations).showNow(parentFragmentManager, BlockConfirmationsBottomSheetDialogFragment.TAG)
        }
        if (asset.assetId == Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) {
            binding.addressTitle.setText(R.string.Invoice)
            binding.lightningRl.isVisible = true
            val address = "${Session.getAccount()?.identityNumber}@mixin.id"
            binding.lightningAddressTv.text = address
            binding.lightningAddressCopy.setOnClickListener {
                context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, address))
                toast(R.string.copied_to_clipboard)
            }

            binding.lightningAddressTip.setOnClickListener {
                LightningAddressBottomSheetDialogFragment.newInstance(address).apply {
                    copyCallback = {
                        context?.getClipboardManager()?.setPrimaryClip(ClipData.newPlainText(null, it))
                        toast(R.string.copied_to_clipboard)
                    }
                }.showNow(parentFragmentManager, LightningAddressBottomSheetDialogFragment.TAG)
            }
        } else {
            binding.addressTitle.setText(R.string.Address)
            binding.lightningRl.isVisible = false
        }
    }
}

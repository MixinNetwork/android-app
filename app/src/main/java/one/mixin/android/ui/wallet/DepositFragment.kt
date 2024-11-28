package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import one.mixin.android.crypto.sha3Sum256
import one.mixin.android.crypto.verifyCurve25519Signature
import one.mixin.android.databinding.FragmentDepositBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.highLight
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.isNullOrEmpty
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.util.ErrorHandler.Companion.ADDRESS_GENERATING
import one.mixin.android.vo.safe.DepositEntry
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class DepositFragment : BaseFragment() {
    companion object {
        const val TAG = "DepositFragment"
    }

    private val notSupportDepositAssets = arrayOf(OMNI_USDT_ASSET_ID, BYTOM_CLASSIC_ASSET_ID, MGD_ASSET_ID)

    private val usdcAssets =
        mapOf(
            "9b180ab6-6abe-3dc0-a13f-04169eb34bfa" to "ERC-20",
            "fe26b981-29e9-3032-a0e9-b24d619e987e" to "TRC-20",
            "de6fa523-c596-398e-b12f-6d6980544b59" to "Solana",
            "2f845564-3898-3d17-8c24-3275e96235b5" to "Base",
            "5fec1691-561d-339f-8819-63d54bf50b52" to "Polygon",
            "3d3d69f1-6742-34cf-95fe-3f8964e6d307" to "BEP-20"
        )

    private val usdtAssets =
        mapOf(
            "4d8c508b-91c5-375b-92b0-ee702ed2dac5" to "ERC-20",
            "b91e18ff-a9ae-3dc7-8679-e935d9a4b34b" to "TRC-20",
            "cb54aed4-1893-3977-b739-ec7b2e04f0c5" to "Solana",
            "218bc6f4-7927-3f8e-8568-3a3725b74361" to "Polygon",
            "94213408-4ee7-3150-a9c4-9c5cce421c78" to "BEP-20",
        )

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
        val asset = requireNotNull(requireArguments().getParcelableCompat(TransactionsFragment.ARGS_ASSET, TokenItem::class.java)) { "required TokenItem can not be null" }
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
                rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.DEPOSIT) }
            }
            title.setSubTitle(getString(R.string.Deposit), asset.symbol)
            if (notSupport) {
                notSupportLl.isVisible = true
                sv.isVisible = false
                val symbol = if (asset.assetId == OMNI_USDT_ASSET_ID) "OMNI-USDT" else asset.symbol
                val info = getString(R.string.not_supported_deposit, symbol, symbol)
                val url = Constants.HelpLink.DEPOSIT_NOT_SUPPORT
                notSupportTv.highlightStarTag(info, arrayOf(url))
            } else {
                if (usdtAssets.contains(asset.assetId)) {
                    networkTitle.isVisible = true
                    networkChipGroup.isVisible = true
                    initUsdtChips(asset)
                } else if (usdcAssets.contains(asset.assetId)){
                    networkTitle.isVisible = true
                    networkChipGroup.isVisible = true
                    initUsdcChips(asset)
                } else {
                    networkTitle.isVisible = false
                    networkChipGroup.isVisible = false
                }

                notSupportLl.isVisible = false
                sv.isVisible = true
                val dustTip =
                    if (asset.hasDust()) {
                        getString(R.string.deposit_dust, asset.dust, asset.symbol)
                            .highLight(requireContext(), "${asset.dust} ${asset.symbol}")
                    } else {
                        SpannableStringBuilder()
                    }
                val confirmation =
                    requireContext().resources.getQuantityString(
                        R.plurals.deposit_confirmation,
                        asset.confirmations,
                        asset.confirmations,
                    )
                        .highLight(requireContext(), asset.confirmations.toString())
                tipTv.text =
                    buildBulletLines(
                        requireContext(),
                        SpannableStringBuilder(getTipsByAsset(asset)),
                        confirmation,
                        dustTip,
                    )
            }
        }

        if (!notSupport) {
            refreshDeposit(asset)
        }
    }

    private val localMap = mutableMapOf<String, DepositEntry>()

    private fun initUsdtChips(asset: TokenItem) {
        initChips(asset, usdtAssets)
    }

    private fun initUsdcChips(asset: TokenItem) {
        initChips(asset, usdcAssets)
    }

    private fun initChips(asset: TokenItem, uAssets: Map<String, String>) {
        binding.apply {
            networkChipGroup.isSingleSelection = true
            networkChipGroup.removeAllViews()
            uAssets.entries.forEach { entry ->
                val chip =
                    Chip(requireContext()).apply {
                        text = entry.value
                        isClickable = true
                        val same = entry.key == asset.assetId
                        if (same) {
                            isChecked = true
                            setTextColor(Color.WHITE)
                            chipBackgroundColor = ColorStateList.valueOf(Color.BLACK)
                        } else {
                            setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
                            chipBackgroundColor = ColorStateList.valueOf(requireContext().colorFromAttribute(R.attr.bg_gray_light))
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
                                        initChips(newAsset, usdcAssets)
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
            val (depositEntry, different, code) = walletViewModel.findAndSyncDepositEntry(asset.chainId)
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
                if (different) {
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
            addressView.isVisible = false
            addressTitle.isVisible = false
            tipTv.isVisible = false
            memoTitle.isVisible = false
            memoView.isVisible = false
        }
    }

    private fun hideLoading() {
        binding.apply {
            loading.isVisible = false
            addressView.isVisible = true
            addressTitle.isVisible = true
            tipTv.isVisible = true
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
        val pub = Constants.SAFE_PUBLIC_KEY.hexStringToByteArray()
        val message =
            if (tag.isNullOrBlank()) {
                destination
            } else {
                "$destination:$tag"
            }.toByteArray().sha3Sum256()
        val verify = verifyCurve25519Signature(message, signature, pub)
        if (verify) {
            val noTag = tag.isNullOrBlank()
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
                )
            }
        } else {
            binding.apply {
                notSupportLl.isVisible = true
                sv.isVisible = false
                notSupportTv.setText(R.string.verification_failed)
            }
        }
    }
}

package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.AssetId.BYTOM_CLASSIC_ASSET_ID
import one.mixin.android.Constants.AssetId.MGD_ASSET_ID
import one.mixin.android.Constants.AssetId.OMNI_USDT_ASSET_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDepositBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.buildBulletLines
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.highLight
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.TokenItem
import one.mixin.android.vo.needShowReserve
import timber.log.Timber

@AndroidEntryPoint
class DepositFragment : BaseFragment() {

    companion object {
        const val TAG = "DepositFragment"
    }

    private val notSupportDepositAssets = arrayOf(OMNI_USDT_ASSET_ID, BYTOM_CLASSIC_ASSET_ID, MGD_ASSET_ID)

    private val usdtAssets = mapOf(
        "4d8c508b-91c5-375b-92b0-ee702ed2dac5" to "ERC-20",
        "b91e18ff-a9ae-3dc7-8679-e935d9a4b34b" to "TRON(TRC-20)",
        "5dac5e28-ad13-31ea-869f-41770dfcee09" to "EOS",
        "218bc6f4-7927-3f8e-8568-3a3725b74361" to "Polygon",
        "94213408-4ee7-3150-a9c4-9c5cce421c78" to "BEP-20",
    )

    private var _binding: FragmentDepositBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val walletViewModel by viewModels<WalletViewModel>()

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    private var alertDialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepositBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val asset = requireArguments().getAsset()
        initView(asset)
        if (!notSupportDepositAssets.any { it == asset.assetId }) {
            DepositChooseNetworkBottomSheetDialogFragment.newInstance(asset = asset)
                .apply {
                    this.callback = {
                        val noTag = asset.getTag().isNullOrBlank()
                        if (noTag.not()) {
                            alertDialogBuilder()
                                .setTitle(R.string.Notice)
                                .setCancelable(false)
                                .setMessage(getString(R.string.deposit_notice, asset.symbol))
                                .setPositiveButton(R.string.OK) { dialog, _ ->
                                    dialog.dismiss()
                                }.show()
                        }
                    }
                }
                .showNow(childFragmentManager, TAG)
        }
        lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
            Timber.e(error)
        }){
            // todo save deposit
            walletViewModel.createDeposit(asset.chainId, asset.assetId).let {
                Timber.e(it.data?.first()?.destination)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alertDialog?.dismiss()
        alertDialog = null
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
                } else {
                    networkTitle.isVisible = false
                    networkChipGroup.isVisible = false
                }

                notSupportLl.isVisible = false
                sv.isVisible = true
                val reserveTip = if (asset.needShowReserve()) {
                    getString(R.string.deposit_reserve, "${asset.reserve} ${asset.symbol}")
                        .highLight(requireContext(), "${asset.reserve} ${asset.symbol}")
                } else {
                    SpannableStringBuilder()
                }
                val confirmation = requireContext().resources.getQuantityString(
                    R.plurals.deposit_confirmation,
                    asset.confirmations,
                    asset.confirmations,
                )
                    .highLight(requireContext(), asset.confirmations.toString())
                tipTv.text = buildBulletLines(
                    requireContext(),
                    SpannableStringBuilder(getTipsByAsset(asset)),
                    confirmation,
                    reserveTip,
                )
            }
        }

        if (!notSupport) {
            updateUI(asset)
            refreshAsset(asset)
        }
    }

    private fun initUsdtChips(asset: TokenItem) {
        binding.apply {
            networkChipGroup.isSingleSelection = true
            networkChipGroup.removeAllViews()
            usdtAssets.entries.forEach { entry ->
                val chip = Chip(requireContext()).apply {
                    text = entry.value
                    isClickable = true
                    val same = entry.key == asset.assetId
                    if (same) {
                        isChecked = true
                        setTextColor(Color.WHITE)
                        chipBackgroundColor = ColorStateList.valueOf(Color.BLACK)
                    } else {
                        setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                        chipBackgroundColor = ColorStateList.valueOf(requireContext().colorFromAttribute(R.attr.bg_gray_light))
                    }
                    setOnClickListener {
                        if (same) return@setOnClickListener

                        lifecycleScope.launch {
                            var newAsset = walletViewModel.findAssetItemById(entry.key)
                            if (newAsset == null) {
                                alertDialog?.dismiss()
                                alertDialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                    show()
                                }
                                newAsset = walletViewModel.findOrSyncAsset(entry.key)
                                alertDialog?.dismiss()
                            }
                            if (newAsset == null) {
                                toast(R.string.Not_found)
                            } else {
                                initView(newAsset)
                            }
                        }
                    }
                }
                networkChipGroup.addView(chip)
            }
        }
    }

    private fun refreshAsset(asset: TokenItem) {
        if (asset.getDestination().isNotBlank()) return

        lifecycleScope.launch {
            val assetItem = walletViewModel.findOrSyncAsset(asset.assetId)
            if (assetItem == null) {
                delay(500)
                refreshAsset(asset)
            } else {
                updateUI(assetItem)
            }
        }
    }

    private fun Bundle.getAsset(): TokenItem = requireNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(TransactionsFragment.ARGS_ASSET, TokenItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(TransactionsFragment.ARGS_ASSET)
        },
    ) { "required AssetItem can not be null" }

    private fun updateUI(asset: TokenItem) {
        if (viewDestroyed()) return

        val noTag = asset.getTag().isNullOrBlank()
        binding.apply {
            if (noTag) {
                memoView.isVisible = false
                memoTitle.isVisible = false
            } else {
                memoView.isVisible = true
                memoTitle.isVisible = true
                memoView.setAsset(
                    parentFragmentManager,
                    scopeProvider,
                    asset,
                    null,
                    true,
                    getString(R.string.deposit_memo_notice),
                )
            }
            addressView.setAsset(
                parentFragmentManager,
                scopeProvider,
                asset,
                null,
                false,
                if (noTag) null else getString(R.string.deposit_notice, asset.symbol),
            )
        }
    }
}

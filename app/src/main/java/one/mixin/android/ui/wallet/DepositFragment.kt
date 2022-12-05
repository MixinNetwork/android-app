package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import dagger.hilt.android.AndroidEntryPoint
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
import one.mixin.android.extension.getTipsByAsset
import one.mixin.android.extension.highLight
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.needShowReserve

@AndroidEntryPoint
class DepositFragment : BaseFragment() {

    companion object {
        const val TAG = "DepositFragment"
    }

    private val notSupportDepositAssets = arrayOf(OMNI_USDT_ASSET_ID, BYTOM_CLASSIC_ASSET_ID, MGD_ASSET_ID)

    private var _binding: FragmentDepositBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val walletViewModel by viewModels<WalletViewModel>()

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepositBinding.inflate(inflater, container, false).apply { this.root.setOnClickListener { } }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val asset = requireArguments().getAsset()
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
                val info = getString(R.string.not_supported_deposit, asset.symbol, asset.symbol)
                val url = Constants.HelpLink.DEPOSIT_NOT_SUPPORT
                notSupportTv.highlightStarTag(info, arrayOf(url))
            } else {
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
                    asset.confirmations
                )
                    .highLight(requireContext(), asset.confirmations.toString())
                tipTv.text = buildBulletLines(
                    requireContext(),
                    SpannableStringBuilder(getTipsByAsset(asset)),
                    confirmation,
                    reserveTip
                )
            }
        }
        if (!notSupport) {
            updateUI(asset)
            refreshAsset(asset)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshAsset(asset: AssetItem) {
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

    private fun Bundle.getAsset(): AssetItem = requireNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(TransactionsFragment.ARGS_ASSET, AssetItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(TransactionsFragment.ARGS_ASSET)
        }
    ) { "required AssetItem can not be null" }

    private fun updateUI(asset: AssetItem) {
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
                    asset,
                    true,
                    getString(R.string.deposit_memo_notice)
                )
            }
            addressView.setAsset(
                parentFragmentManager,
                asset,
                false,
                if (noTag) null else getString(R.string.deposit_notice, asset.symbol)
            )
        }
    }
}

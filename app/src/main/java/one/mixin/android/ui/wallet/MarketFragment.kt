package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.LineChart
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.safe.TokenItem
import javax.inject.Inject

@AndroidEntryPoint
class MarketFragment : BaseFragment(R.layout.fragment_market) {
    companion object {
        const val TAG = "MarketFragment"
    }

    private val binding by viewBinding(FragmentMarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    lateinit var asset: TokenItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)!!
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(asset.assetId))))
        binding.titleView.apply {
            val sub = getChainName(asset.chainId, asset.chainName, asset.assetKey)
            if (sub != null)
                setSubTitle(asset.name, sub)
            else
                titleTv.text = asset.name
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
        binding.apply {
            icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            icon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            market.setContent {
                LineChart(listOf(1.0f, 2.3f, 2.0f, 6.2f, 7.8f, 5.2f, 4.5f, 5.5f, 5.0f, 4.2f, 3.5f, 4.5f, 4.0f), Color(0xFF50BD5CL), true)
            }
        }
    }

}

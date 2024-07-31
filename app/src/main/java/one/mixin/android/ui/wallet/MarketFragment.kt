package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
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

    @SuppressLint("SetTextI18n")
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
                LineChart(listOf(3119.10f, 3219.3f, 3301.02f, 3250.2f, 3270.8f, 3240.2f, 3110.5f, 3201.5f, 3500.0f, 3421.2f, 3321.5f, 3214.5f, 3321.54f), Color(0xFF50BD5CL), true)
            }

            // Todo real data
            marketHigh.text = "\$3,196.59"
            marketLow.text = "\$2,810.00"
            marketVolC.text = "\$3,196.59"
            marketVolU.text = "2.47B"
            balance.text = "\$3309.21"
            value.text = "\$3309.21"
            rise.text = "+2.34%"

            name.text = asset.name
            symbol.text = asset.symbol
            chain.text = asset.chainName

            introduction.text = "Ethereum was created in 2015 by Vitalik Buterin, a Russian-Canadian programmer. The platform is based on the principle of decentralization, which means that it is not controlled by any single entity"

            address.text = asset.assetKey

            marketCap.text = "$343.75B"
            circulationSupply.text = "120.2M ETH"
            totalSupply.text = "120.2M ETH"
            issueDate.text = "2024-07-24"
            issuePrice.text = "$0.308"
            highValue.text = "$4,891.7047"
            highTime.text = "2021-11-16"
            lowValue.text = "$0.420897"
            lowTime.text = "2015-11-16"
        }
    }
}

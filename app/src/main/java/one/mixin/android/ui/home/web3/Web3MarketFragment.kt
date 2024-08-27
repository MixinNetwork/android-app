package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.databinding.FragmentWeb3MarketBinding
import one.mixin.android.databinding.ItemWeb3MarketBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.market.Web3Market
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Web3MarketFragment : BaseFragment(R.layout.fragment_web3_market) {
    companion object {
        const val TAG = "MarketFragment"
    }

    private val binding by viewBinding(FragmentWeb3MarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            rv.adapter = adapter
        }

        lifecycleScope.launch {
            requestRouteAPI(
                invokeNetwork = { walletViewModel.web3Markets() },
                successBlock = { response ->
                    if (response.isSuccess) {
                        adapter.items = response.data!!
                        adapter.notifyDataSetChanged()
                    }
                },
                requestSession = { walletViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            )
        }
    }

    private val adapter by lazy {
        Web3MarketAdapter()
    }


    class Web3MarketAdapter() : RecyclerView.Adapter<Web3MarketAdapter.ViewHolder>() {
        var items: List<Web3Market> = emptyList()

        class ViewHolder(val binding: ItemWeb3MarketBinding) : RecyclerView.ViewHolder(binding.root) {
            @SuppressLint("CheckResult")
            fun bind(item:Web3Market){
                binding.apply {
                    icon.loadImage(item.iconUrl, R.drawable.ic_avatar_place_holder)
                    assetName.text = item.name
                    assetValue.text = item.totalVolume
                    price.text = item.currentPrice
                    market.loadImage("https://www.coingecko.com/coins/${item.marketCapRank}/sparkline.svg".apply {
                        Timber.e(this)
                    })
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemWeb3MarketBinding.inflate(LayoutInflater.from(parent.context)))
        }

        // Bind data to item views
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        // Return the size of the dataset
        override fun getItemCount(): Int = items.size
    }

}
package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBuyCryptoBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.checkout.TraceRequest
import timber.log.Timber

@AndroidEntryPoint
class BuyCryptoFragment : BaseFragment(R.layout.fragment_buy_crypto) {
    companion object {
        const val TAG = "BuyCryptoFragment"
        const val ARGS_CURRENCY = "args_currency"

        fun newInstance(assetItem: AssetItem, currency: Currency) = BuyCryptoFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
            putParcelable(ARGS_CURRENCY, currency)
        }
    }

    private val binding by viewBinding(FragmentBuyCryptoBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = requireNotNull(requireArguments().getParcelableCompat(ARGS_ASSET, AssetItem::class.java))
        currency = requireNotNull(requireArguments().getParcelableCompat(ARGS_CURRENCY, Currency::class.java))
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            continueTv.setOnClickListener {
                checkToken()
            }
            titleView.rightAnimator.setOnClickListener { }
            updateUI()
            assetRl.setOnClickListener {
                AssetListBottomSheetDialogFragment.newInstance(false)
                    .setOnAssetClick { asset ->
                        this@BuyCryptoFragment.asset = asset
                        updateUI()
                    }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
            }
            fiatRl.setOnClickListener {
                FiatListBottomSheetDialogFragment.newInstance(currency).apply {
                    callback = object : FiatListBottomSheetDialogFragment.Callback {
                        override fun onCurrencyClick(currency: Currency) {
                            this@BuyCryptoFragment.currency = currency
                            updateUI()
                        }
                    }
                }.showNow(parentFragmentManager, FiatListBottomSheetDialogFragment.TAG)
            }
        }
    }

    private fun updateUI() {
        binding.apply {
            titleView.setSubTitle(
                getString(R.string.Buy_asset, asset.symbol),
                requireNotNull(getChainName(asset.chainId, asset.chainName, asset.assetKey)) {
                    "required chain name must not be null"
                },
            )
            assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            assetName.text = asset.name
            assetDesc.text = asset.balance.numberFormat()
            descEnd.text = asset.symbol
            payDesc.text = getString(R.string.Gateway_fee_price, "1.99%")
            fiatAvatar.setImageResource(currency.flag)
            fiatName.text = currency.name
        }
    }

    private fun checkToken() = lifecycleScope.launch {
        binding.innerVa.displayedChild = 1
        // Todo
        navTo(PaymentFragment().apply {
            onSuccess = { token->
                parentFragmentManager.beginTransaction().remove(this).commitNow()
                placeOrder(token)
            }
        }, PaymentFragment.TAG)
    }

    private fun placeOrder(token: String) = lifecycleScope.launch {
        val response = walletViewModel.payment(
            TraceRequest(
                token,
                "USD",
                requireNotNull(Session.getAccountId()),
                1,
                "965e5c6e-434c-3fa9-b780-c50f43cd955c",
            ),
        )
        binding.innerVa.displayedChild = 0
        Timber.e(response.traceID)
    }
}

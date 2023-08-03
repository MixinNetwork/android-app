package one.mixin.android.ui.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.CardRequirements
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBuyCryptoBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.demo.CheckoutActivity
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
        asset = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        currency = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
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
        fetchCanUseGooglePay()
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

    lateinit var getScanResult: ActivityResultLauncher<String>
    private lateinit var resultRegistry: ActivityResultRegistry
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!::resultRegistry.isInitialized) resultRegistry = requireActivity().activityResultRegistry

        getScanResult = registerForActivityResult(CheckoutActivity.PayContract(), resultRegistry, ::callbackPay)
    }

    private fun callbackPay(data: Intent?) {
        val token = data?.getStringExtra("Token")
        Timber.e(token)
    }

    private fun checkToken() = lifecycleScope.launch {
        if (!googlePayAvailable)return@launch
        binding.innerVa.displayedChild = 1
        // Todo
        // navTo(
        //     PaymentFragment().apply {
        //         onSuccess = { token ->
        //             parentFragmentManager.beginTransaction().remove(this).commitNow()
        //             placeOrder(token)
        //         }
        //     },
        //     PaymentFragment.TAG,
        // )

        // val task = getLoadPaymentDataTask()
        // task.addOnCompleteListener { completedTask ->
        //     if (completedTask.isSuccessful) {
        //         completedTask.result.let(::handlePaymentSuccess)
        //         Timber.e("Success")
        //         requireActivity().onBackPressedDispatcher.onBackPressed()
        //     } else {
        //         val exception = completedTask.exception
        //         Timber.e(exception)
        //     }
        // }

        getScanResult.launch("")
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson()
        Timber.e(paymentInformation)
    }

    private val paymentsClient: PaymentsClient = PaymentsUtil.createPaymentsClient(MixinApplication.appContext)
    private var googlePayAvailable = false
    private fun fetchCanUseGooglePay() {
        val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest()
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
        val task = paymentsClient.isReadyToPay(request)

        task.addOnCompleteListener { completedTask ->
            try {
                googlePayAvailable = completedTask.getResult(ApiException::class.java)
                Log.w("isReadyToPay ", "$googlePayAvailable")
            } catch (exception: ApiException) {
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    private val resolvePaymentForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                ComponentActivity.RESULT_OK ->
                    result.data?.let { intent ->
                        PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                    }

                ComponentActivity.RESULT_CANCELED -> {
                    // The user cancelled the payment attempt
                }
            }
        }

    private fun getLoadPaymentDataTask(): Task<PaymentData> {
        val request = PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice("10.00")
                    .setCurrencyCode("USD")
                    .build(),
            )
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .setCardRequirements(
                CardRequirements.newBuilder()
                    .addAllowedCardNetworks(
                        listOf(
                            WalletConstants.CARD_NETWORK_VISA,
                            WalletConstants.CARD_NETWORK_MASTERCARD,
                        ),
                    )
                    .build(),
            )
        val params = PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY,
            )
            .addParameter("gateway", "checkoutltd")
            .addParameter("gatewayMerchantId", BuildConfig.MERCHANT_ID)
            .build()
        request.setPaymentMethodTokenizationParameters(params)
        return paymentsClient.loadPaymentData(request.build())
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

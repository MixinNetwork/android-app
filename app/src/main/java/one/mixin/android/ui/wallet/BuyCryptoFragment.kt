package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.CheckoutApiServiceFactory
import com.checkout.threeds.Checkout3DSService
import com.checkout.threeds.Environment
import com.checkout.threeds.domain.model.AuthenticationError
import com.checkout.threeds.domain.model.AuthenticationErrorType
import com.checkout.threeds.domain.model.AuthenticationParameters
import com.checkout.threeds.domain.model.AuthenticationResult
import com.checkout.threeds.domain.model.ResultType
import com.checkout.tokenization.model.GooglePayTokenRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.CHECKOUT_ENVIRONMENT
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentBuyCryptoBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navTo
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.checkout.TraceRequest
import timber.log.Timber
import java.util.Locale

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
    private var isGooglePay: Boolean = true

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
            buyVa.setOnClickListener {
                when (buyVa.displayedChild) {
                    0 -> {
                        payWithGoogle()
                    }
                    1 -> {
                        payWithCheckout()
                    }
                    else -> {
                        // do noting
                    }
                }
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
            payRl.setOnClickListener {
                ChoosePaymentBottomSheetDialogFragment.newInstance(isGooglePay).apply {
                    onPaymentClick = { isGooglePay ->
                        this@BuyCryptoFragment.isGooglePay = isGooglePay
                        updateUI()
                    }
                }.show(parentFragmentManager, ChoosePaymentBottomSheetDialogFragment.TAG)
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

    // Todo 3DS
    private fun init3DS() {
        val checkout3DS = Checkout3DSService(
            MixinApplication.appContext,
            Environment.SANDBOX,
            Locale.UK,
            null,
            null, // mixin://
        )

        val authenticationParameters = AuthenticationParameters(
            "", //   sessionId,
            "", // sessionSecret,
            "",
        ) // scheme)

        checkout3DS.authenticate(authenticationParameters) { result: AuthenticationResult ->
            when (result.resultType) {
                ResultType.Completed -> {
                    // continue with payment, show √
                }

                ResultType.Error -> {
                    // handle error (result as AuthenticationError)

                    // handle error based on error type category
                    val errorType: AuthenticationErrorType =
                        (result as AuthenticationError).errorType

                    // Handle error based on fine grained error code or simply log the error
                    val errorCode: String = result.errorCode
                    showError(errorCode)
                }
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
            payName.text =
                if (isGooglePay) getString(R.string.Google_Pay) else getString(R.string.Visa_Mastercard)
            payAvatar.setImageResource(if (isGooglePay) R.drawable.ic_google_pay else R.drawable.ic_visa)
            payDesc.text = getString(R.string.Gateway_fee_price, "1.99%")
            fiatAvatar.setImageResource(currency.flag)
            fiatName.text = currency.name

            binding.buyVa.displayedChild = if (isGooglePay) {
                0
            } else {
                1
            }

            // Todo real data
            price.tail.text = "0.995 USD / USDC"
            gatewayFee.tail.text = "1.123 USD"
            networkFee.tail.text = "0 USD"
        }
    }

    private fun payWithCheckout() = lifecycleScope.launch {
        binding.buyVa.displayedChild = 3
        navTo(
            PaymentFragment().apply {
                onSuccess = { token ->
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                        .remove(this).commitNow()
                    placeOrder(token, "") // todo
                }
                onFailure = {
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                        .remove(this).commitNow()
                    showError(it)
                }
            },
            PaymentFragment.TAG,
        )
    }

    private fun payWithGoogle() {
        // Todo real data
        val task = walletViewModel.getLoadPaymentDataTask("1.00", "USD")

        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                completedTask.result.let(::handlePaymentSuccess)
            } else {
                when (val exception = completedTask.exception) {
                    is ResolvableApiException -> {
                        resolvePaymentForResult.launch(
                            IntentSenderRequest.Builder(exception.resolution).build(),
                        )
                    }

                    is ApiException -> {
                        handleError(exception.statusCode, exception.message)
                    }

                    else -> {
                        handleError(
                            CommonStatusCodes.INTERNAL_ERROR,
                            "Unexpected non API" +
                                " exception when trying to deliver the task result to an activity!",
                        )
                    }
                }
            }

            binding.payTv.isEnabled = true
        }
    }

    private fun placeOrder(token: String, sessionId: String) = lifecycleScope.launch {
        // todo real data
        val response = walletViewModel.payment(
            TraceRequest(
                token,
                "USD",
                requireNotNull(Session.getAccountId()),
                1,
                "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                sessionId,
                "",
            ),
        )
        walletViewModel.paymentState(response.traceID).let {
            Timber.e(it)
        }
        OrderPreviewBottomSheetDialogFragment.newInstance(
            AssetItem(
                assetId = "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                assetKey = "0xec2a0550a2e4da2a027b3fc06f70ba15a94a6dac",
                balance = "18.6818173",
                chainIconUrl = "https://mixin-images.zeromesh.net/zVDjOxNTQvVsA8h2B4ZVxuHoCF3DJszufYKWpd9duXUSbSapoZadC7_13cnWBqg0EmwmRcKGbJaUpA8wFfpgZA\u003ds128",
                chainId = "43d61dcd-e413-450d-80b8-101d5e903357",
                chainName = "Ethereum",
                chainPriceUsd = "1854.39",
                chainSymbol = "ETH",
                changeBtc = "-0.025177743202846662",
                changeUsd = "0.0040655737704918034",
                confirmations = 32,
                depositEntries = null,
                destination = "0x45315C1Fd776AF95898C77829f027AFc578f9C2B",
                hidden = false,
                iconUrl = "https://mixin-images.zeromesh.net/0sQY63dDMkWTURkJVjowWY6Le4ICjAFuu3ANVyZA4uI3UdkbuOT5fjJUT82ArNYmZvVcxDXyNjxoOv0TAYbQTNKS\u003ds128",
                name = "Chui Niu Bi",
                priceBtc = "0",
                priceUsd = "0",
                reserve = "0",
                symbol = "CNB",
                tag = "",
                withdrawalMemoPossibility = WithdrawalMemoPossibility.NEGATIVE,
            ),
        ).show(parentFragmentManager, OrderPreviewBottomSheetDialogFragment.TAG)
        updateUI()
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        try {
            val tokenJsonPayload = paymentData.paymentMethodToken?.token
            if (tokenJsonPayload != null) {
                CheckoutApiServiceFactory.create(
                    BuildConfig.CHCEKOUT_ID,
                    CHECKOUT_ENVIRONMENT,
                    requireContext(),
                ).createToken(
                    GooglePayTokenRequest(tokenJsonPayload, { tokenDetails ->
                        // todo create session and 3ds
                        placeOrder(tokenDetails.token, sessionId = "")
                    }, {
                        showError(it)
                    }),
                )
            } else {
                showError("Token null")
            }
        } catch (error: Exception) {
            showError(error.message)
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
                    toast(R.string.Cancel)
                    showError()
                }
            }
        }

    private fun handleError(statusCode: Int, message: String?) {
        showError(message)
    }

    private fun showError(message: String?) {
        if (!isAdded) return
        // Todo display Error page
    }
    private fun showError(@StringRes errorRes: Int = R.string.Unknown) {
        showError(getString(errorRes))
    }
}

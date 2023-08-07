package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.CheckoutApiServiceFactory
import com.checkout.base.model.Environment
import com.checkout.threeds.Checkout3DSService
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
            innerVa.setOnClickListener {
                checkToken()
            }
            payTv.setOnClickListener {
                payWithGoogle()
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
                // Todo check kyc
                // view.navigate(
                //     R.id.action_wallet_to_identity,
                // )
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
            com.checkout.threeds.Environment.SANDBOX,
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
                    val errorType: AuthenticationErrorType = (result as AuthenticationError).errorType

                    // Handle error based on fine grained error code or simply log the error
                    val errorCode: String = (result as AuthenticationError).errorCode
                    // Todo display Error page
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
            payName.text = if (isGooglePay) getString(R.string.Google_Pay) else getString(R.string.Visa_Mastercard)
            payAvatar.setImageResource(if (isGooglePay) R.drawable.ic_google_pay else R.drawable.ic_visa)
            payDesc.text = getString(R.string.Gateway_fee_price, "1.99%")
            fiatAvatar.setImageResource(currency.flag)
            fiatName.text = currency.name

            binding.payTv.isVisible = isGooglePay
            binding.innerVa.isVisible = !isGooglePay

            // Todo real data
            price.tail.text = "0.995 USD / USDC"
            gatewayFee.tail.text = "1.123 USD"
            networkFee.tail.text = "0 USD"
        }
    }

    private fun checkToken() = lifecycleScope.launch {
        binding.innerVa.displayedChild = 1
        navTo(
            PaymentFragment().apply {
                onSuccess = { token ->
                    parentFragmentManager.beginTransaction().remove(this).commitNow()
                    placeOrder(token)
                }
            },
            PaymentFragment.TAG,
        )
    }

    private fun payWithGoogle() {
        binding.payTv.isEnabled = false
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

    private fun placeOrder(token: String) = lifecycleScope.launch {
        // todo real data
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
        // Todo show trace page
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        try {
            val tokenJsonPayload = paymentData.paymentMethodToken?.token
            if (tokenJsonPayload != null) {
                CheckoutApiServiceFactory.create(
                    BuildConfig.CHCEKOUT_ID,
                    Environment.SANDBOX,
                    requireContext(),
                ).createToken(
                    GooglePayTokenRequest(tokenJsonPayload, { tokenDetails ->
                        placeOrder(tokenDetails.token)
                    }, {
                        // Todo display Error page
                    }),
                )
            } else {
                // Todo display Error page
            }
        } catch (error: Exception) {
            // Todo display Error page
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
                    // Todo display Error page
                }
            }
        }

    private fun handleError(statusCode: Int, message: String?) {
        // Todo display Error page
    }
}

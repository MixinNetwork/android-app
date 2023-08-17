package one.mixin.android.ui.wallet.fiatmoney

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.api.request.SessionStatus
import one.mixin.android.api.response.CreateSessionResponse
import one.mixin.android.databinding.FragmentOrderConfirmBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.ErrorFragment
import one.mixin.android.ui.wallet.OrderPreviewBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.checkout.PaymentRequest
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class OrderConfirmFragment : BaseFragment(R.layout.fragment_order_confirm) {
    companion object {
        const val TAG = "OrderConfirmFragment"
        const val ARGS_CURRENCY = "args_currency"
        const val ARGS_GOOGLE_PAY = "args_google_pay"
        const val ARGS_SCHEME = "args_scheme"
        const val ARGS_INSTRUMENT_ID = "args_instrument_id"
        const val ARGS_AMOUNT = "args_amount"

        fun newInstance(assetItem: AssetItem, currency: Currency) =
            OrderConfirmFragment().withArgs {
                putParcelable(TransactionsFragment.ARGS_ASSET, assetItem)
                putParcelable(ARGS_CURRENCY, currency)
            }
    }

    private val binding by viewBinding(FragmentOrderConfirmBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency
    private var isGooglePay: Boolean = false

    private val amount by lazy {
        requireArguments().getInt(ARGS_AMOUNT, 0)
    }

    private val instrumentId by lazy {
        requireArguments().getString(ARGS_INSTRUMENT_ID)
    }

    private val scheme by lazy {
        requireArguments().getString(ARGS_SCHEME)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        currency = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        isGooglePay = requireArguments().getBoolean(
            ARGS_GOOGLE_PAY,
            false,
        )
        binding.apply {
            transparentMask.isVisible = false
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            buyVa.setOnClickListener {
                when (buyVa.displayedChild) {
                    0 -> {
                        payWithCheckout()
                    }

                    1 -> {
                        payWithGoogle()
                    }

                    else -> {
                        // do noting
                    }
                }
            }
            titleView.rightAnimator.setOnClickListener { }
            updateUI()
        }
    }

    // Todo 3DS
    private fun init3DS(sessionResponse: CreateSessionResponse) {
        val checkout3DS = Checkout3DSService(
            MixinApplication.appContext,
            Environment.SANDBOX,
            Locale.US,
            null,
            null, // mixin://
        )

        val authenticationParameters = AuthenticationParameters(
            sessionResponse.sessionId,
            sessionResponse.sessionSecret,
            sessionResponse.scheme,
        )


        checkout3DS.authenticate(authenticationParameters) { result: AuthenticationResult ->
            when (result.resultType) {
                ResultType.Completed -> {
                    // continue with payment, show √
                    Timber.e("Completed")
                    lifecycleScope.launch {
                        while (true) {
                            delay(1000)
                            val session = try {
                                walletViewModel.getSession(sessionResponse.sessionId)
                            } catch (e: Exception) {
                                showError(e.message)
                                break
                            }
                            if (session.data?.status == SessionStatus.Approved.value) {
                                placeOrder(
                                    sessionId = sessionResponse.sessionId,
                                    sessionResponse.instrumentId
                                )
                                break
                            } else if (session.data?.status != SessionStatus.Pending.value || session.data?.status != SessionStatus.Processing.value) {
                                showError(session.data?.status ?: session.errorDescription)
                                break
                            }
                        }
                    }
                }

                ResultType.Error -> {
                    // handle error (result as AuthenticationError)

                    // handle error based on error type category
                    val errorType: AuthenticationErrorType =
                        (result as AuthenticationError).errorType

                    // Handle error based on fine grained error code or simply log the error
                    val errorCode: String = result.errorCode

                    Timber.e("Error $errorType $errorCode")
                    showError(errorCode)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.onBackPressedDispatcher?.addCallback(this, onBackPressedCallback)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.transparentMask.isVisible) {
                // do noting
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun updateUI() {
        binding.apply {
            titleView.setSubTitle(getString(R.string.Order_Confirm), "")

            val toValue = if (isGooglePay) {
                1
            } else {
                0
            }
            if (buyVa.displayedChild != toValue) {
                buyVa.displayedChild = if (isGooglePay) {
                    1
                } else {
                    0
                }

                buyVa.setBackgroundResource(
                    if (isGooglePay) {
                        R.drawable.bg_round_black_btn_40
                    } else {
                        R.drawable.bg_round_blue_btn_40
                    },
                )
            }
            assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)

            // Todo real data
            assetName.text = "+51.23 USDC"
            cardNumber.text = "Visa .... 4242"
            priceTv.text = "1 USD = 0.995 USDC"
            purchaseTv.text = "48.78 USD"
            feeTv.text = "1.23 USD"
            totalTv.text = "50 USD"
        }
    }

    private fun payWithCheckout() = lifecycleScope.launch {
        binding.buyVa.displayedChild = 2
        binding.transparentMask.isVisible = true
        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    walletViewModel.createSession(
                        CreateSessionRequest(
                            null,
                            currency.name,
                            scheme,
                            Session.getAccountId()!!,
                            "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                            amount,
                            instrumentId,
                        ),
                    )
                },
                successBlock = { response ->
                    if (response.isSuccess) {
                        init3DS(response.data!!)
                    } else {
                        showError(response.errorDescription)
                    }
                },
            )
        }
    }

    private fun payWithGoogle() {
        binding.buyVa.displayedChild = 2
        binding.transparentMask.isVisible = true
        val task = walletViewModel.getLoadPaymentDataTask("${amount / 100f}", currency.name)
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
        }
    }

    private fun placeOrder(sessionId: String, instrumentId: String) = lifecycleScope.launch {
        // todo real data
        try {
            val response = walletViewModel.payment(
                PaymentRequest(
                    "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                    Session.getAccountId()!!,
                    sessionId,
                    instrumentId,
                    amount.toLong(),
                    currency.name,
                ),
            )
            if (response.isSuccess) {
                binding.transparentMask.isVisible = false
                updateUI()
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
            } else {
                showError(response.errorDescription)
            }
        } catch (e: Exception) {
            showError(e.message)
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        try {
            val tokenJsonPayload = paymentData.paymentMethodToken?.token
            if (tokenJsonPayload != null) {
                CheckoutApiServiceFactory.create(
                    BuildConfig.CHCEKOUT_ID,
                    Constants.CHECKOUT_ENVIRONMENT,
                    requireContext(),
                ).createToken(
                    GooglePayTokenRequest(tokenJsonPayload, { tokenDetails ->
                        lifecycleScope.launch {
                            handleMixinResponse(
                                invokeNetwork = {
                                    walletViewModel.createSession(
                                        CreateSessionRequest(
                                            tokenDetails.token,
                                            currency.name,
                                            tokenDetails.scheme?.lowercase(),
                                            Session.getAccountId()!!,
                                            "965e5c6e-434c-3fa9-b780-c50f43cd955c",
                                            amount,
                                        ),
                                    )
                                },
                                successBlock = { response ->
                                    if (response.isSuccess) {
                                        init3DS(response.data!!)
                                    } else {
                                        showError(response.errorDescription)
                                    }
                                },
                            )
                        }
                    }, {
                        showError(it)
                        binding.transparentMask.isVisible = false
                    }),
                )
            } else {
                showError("Token null")
                binding.transparentMask.isVisible = false
            }
        } catch (error: Exception) {
            showError(error.message)
            binding.transparentMask.isVisible = false
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
                    binding.transparentMask.isVisible = false
                    binding.buyVa.displayedChild = 1
                }
            }
        }

    private fun handleError(statusCode: Int, message: String?) {
        showError(message)
    }

    private fun showError(message: String?) {
        if (!isAdded) return
        ErrorFragment.newInstance(message ?: getString(R.string.Unknown))
            .showNow(parentFragmentManager, ErrorFragment.TAG)
        binding.transparentMask.isVisible = false
        binding.buyVa.displayedChild = if (isGooglePay) {
            1
        } else {
            0
        }
    }

    private fun showError(@StringRes errorRes: Int = R.string.Unknown) {
        showError(getString(errorRes))
    }
}

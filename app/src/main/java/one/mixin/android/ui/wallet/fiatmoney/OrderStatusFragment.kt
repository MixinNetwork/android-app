package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.threeds.Checkout3DSService
import com.checkout.threeds.domain.model.AuthenticationError
import com.checkout.threeds.domain.model.AuthenticationErrorType
import com.checkout.threeds.domain.model.AuthenticationParameters
import com.checkout.threeds.domain.model.AuthenticationResult
import com.checkout.threeds.domain.model.ResultType
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.CRYPTOGRAM_3DS
import one.mixin.android.Constants.RouteConfig.ENVIRONMENT_3DS
import one.mixin.android.Constants.RouteConfig.PAN_ONLY
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.response.RoutePaymentStatus
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteSessionStatus
import one.mixin.android.databinding.FragmentOrderStatusBinding
import one.mixin.android.extension.bold
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.navigate
import one.mixin.android.extension.textColor
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.cardIcon
import one.mixin.android.vo.route.RoutePaymentRequest
import timber.log.Timber
import java.lang.IllegalArgumentException
import java.util.Locale

@AndroidEntryPoint
class OrderStatusFragment : BaseFragment(R.layout.fragment_order_status) {
    companion object {
        const val TAG = "OrderConfirmFragment"
        const val ARGS_CURRENCY = "args_currency"
        const val ARGS_GOOGLE_PAY = "args_google_pay"
        const val ARGS_SCHEME = "args_scheme"
        const val ARGS_INSTRUMENT_ID = "args_instrument_id"
        const val ARGS_AMOUNT = "args_amount"
        const val ARGS_INFO = "args_info"

        private const val REFRESH_INTERVAL = 2000L

        fun newInstance(assetItem: AssetItem, currency: Currency) =
            OrderStatusFragment().withArgs {
                putParcelable(TransactionsFragment.ARGS_ASSET, assetItem)
                putParcelable(ARGS_CURRENCY, currency)
            }
    }

    private val binding by viewBinding(FragmentOrderStatusBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency
    private lateinit var info: OrderInfo
    private var isGooglePay: Boolean = false

    private val amount by lazy {
        requireArguments().getLong(ARGS_AMOUNT, 0)
    }

    private val instrumentId by lazy {
        requireArguments().getString(ARGS_INSTRUMENT_ID)
    }

    private val scheme by lazy {
        requireArguments().getString(ARGS_SCHEME)
    }

    private var assetAmount = ""
    private lateinit var expectancy: String
    private var status = OrderStatus.INITIALIZED
        private set(value) {
            if (field != value) {
                field = value
                when (value) {
                    OrderStatus.PROCESSING -> {
                        processing()
                    }
                    OrderStatus.SUCCESS -> {
                        success()
                    }
                    OrderStatus.FAILED -> {
                        failed()
                    }
                    else -> {
                        // do noting
                    }
                }
            }
        }

    private fun success() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 0
        binding.topVa.displayedChild = 0
        binding.title.setText(R.string.Success)
        binding.content.textColor = requireContext().colorFromAttribute(R.attr.text_primary)
        binding.content.text = getString(R.string.Success_desc, "$assetAmount ${asset.symbol}", asset.symbol)
        binding.content.bold("$assetAmount ${asset.symbol}")
        binding.transparentMask.isVisible = false
    }

    private fun failed() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 1
        binding.topVa.displayedChild = 1
        binding.cancelTv.isVisible = true
        binding.title.setText(R.string.buy_failed)
        binding.transparentMask.isVisible = false
    }

    private fun processing() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 2
        binding.topVa.displayedChild = 2
        binding.title.setText(R.string.Processing)
        binding.content.setText(R.string.Processing_desc)
        binding.transparentMask.isVisible = true
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
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

        val scheme = requireArguments().getString(OrderConfirmFragment.ARGS_SCHEME)
        info = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_INFO,
                OrderInfo::class.java,
            ),
        )
        expectancy = info.assetAmount
        binding.apply {
            bottomVa.setOnClickListener {
                when (bottomVa.displayedChild) {
                    0 -> {
                        view.navigate(R.id.action_wallet_status_to_wallet)
                    }

                    1 -> {
                        view.navigate(
                            R.id.action_wallet_status_to_select,
                            Bundle().apply {
                                putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                                putLong(ARGS_AMOUNT, amount)
                                putParcelable(ARGS_CURRENCY, currency)
                            },
                        )
                    }

                    else -> {
                        // do noting
                    }
                }
            }
            cancelTv.setOnClickListener {
                view.navigate(R.id.action_wallet_status_to_wallet)
            }
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                payWith,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                priceTv,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                feeTv,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                feeMixinTv,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                tokenTv,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                purchaseTotalTv,
                8,
                14,
                1,
                TypedValue.COMPLEX_UNIT_SP,
            )
            val logo = when {
                isGooglePay -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_google_pay_small)
                else -> AppCompatResources.getDrawable(requireContext(), cardIcon(scheme))
            }.also {
                if (isGooglePay) {
                    it?.setBounds(0, 0, 28.dp, 14.dp)
                } else {
                    it?.setBounds(0, 0, 26.dp, 16.dp)
                }
            }
            payWith.setCompoundDrawables(logo, null, null, null)
            payWith.text = if (isGooglePay) {
                "Google Pay"
            } else {
                info.number
            }
            priceTv.text = info.exchangeRate
            feeTv.text = info.feeByGateway
            feeMixinTv.text = info.feeByMixin
            tokenTv.text = "${info.assetAmount} ${asset.symbol}"
            purchaseTotalTv.text = info.purchaseTotal
        }
        if (isGooglePay) {
            payWithGoogle()
        } else {
            payWithCheckout()
        }
    }

    private fun init3DS(sessionResponse: RouteSessionResponse) {
        val checkout3DS = Checkout3DSService(
            MixinApplication.appContext,
            ENVIRONMENT_3DS,
            Locale.US,
            null,
            Uri.parse("mixin://buy"), // May jump back to the purchase interface form uri
        )

        val authenticationParameters = AuthenticationParameters(
            sessionResponse.sessionId,
            sessionResponse.sessionSecret,
            sessionResponse.scheme,
        )

        checkout3DS.authenticate(authenticationParameters) { result: AuthenticationResult ->
            when (result.resultType) {
                ResultType.Completed -> {
                    lifecycleScope.launch(defaultErrorHandler) {
                        while (isActive) {
                            val session = try {
                                fiatMoneyViewModel.getSession(sessionResponse.sessionId)
                            } catch (e: Exception) {
                                showError(e.message)
                                return@launch
                            }
                            if (session.isSuccess) {
                                if (session.data?.status == RouteSessionStatus.Approved.value) {
                                    payments(sessionId = sessionResponse.sessionId, instrumentId = sessionResponse.instrumentId, null)
                                    break
                                } else if (session.data?.status != RouteSessionStatus.Pending.value && session.data?.status != RouteSessionStatus.Processing.value) {
                                    showError(session.data?.status ?: session.errorDescription)
                                    return@launch
                                } else {
                                    delay(REFRESH_INTERVAL)
                                }
                            } else {
                                showError(requireContext().getMixinErrorStringByCode(session.errorCode, session.errorDescription))
                                return@launch
                            }
                        }
                    }
                }

                ResultType.Error -> {
                    val errorType: AuthenticationErrorType =
                        (result as AuthenticationError).errorType
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
            if (status == OrderStatus.PROCESSING) {
                // do noting
            } else if (status == OrderStatus.SUCCESS) {
                view?.navigate(R.id.action_wallet_status_to_wallet)
            } else if (status == OrderStatus.FAILED) {
                view?.navigate(
                    R.id.action_wallet_status_to_select,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                        putLong(ARGS_AMOUNT, amount)
                        putParcelable(ARGS_CURRENCY, currency)
                    },
                )
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun payWithCheckout() = lifecycleScope.launch(defaultErrorHandler) {
        status = OrderStatus.PROCESSING
        createSession(scheme!!, null)
    }

    private fun payWithGoogle() {
        status = OrderStatus.PROCESSING
        val task = fiatMoneyViewModel.getLoadPaymentDataTask(AmountUtil.realAmount(amount, currency.name), currency.name)
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

    private fun retry(sessionId: String?, instrumentId: String?, token: String?, expectancyAssetAmount: String) {
        payments(sessionId, instrumentId, token, expectancyAssetAmount)
    }

    @SuppressLint("SetTextI18n")
    private fun payments(sessionId: String?, instrumentId: String?, token: String?, expectancyAssetAmount: String? = null) = lifecycleScope.launch(defaultErrorHandler) {
        val response = fiatMoneyViewModel.payment(
            RoutePaymentRequest(
                amount,
                currency.name,
                asset.assetId,
                assetAmount = expectancyAssetAmount ?: expectancy,
                token,
                sessionId,
                instrumentId,
            ),
        )
        if (response.isSuccess) {
            if (response.data?.status == RoutePaymentStatus.Captured.name) {
                assetAmount = response.data!!.assetAmount
                status = OrderStatus.SUCCESS
            } else if (response.data?.status == RoutePaymentStatus.Declined.name) {
                showError(getString(R.string.buy_declined_description))
            } else {
                val paymentId = response.data?.paymentId
                if (paymentId == null) {
                    showError(response.errorDescription)
                } else {
                    getPaymentStatus(paymentId)
                }
            }
        } else {
            if (response.errorCode == ErrorHandler.EXPIRED_PRICE) {
                val extra = response.error?.extra?.asJsonObject?.get("data")?.asJsonObject
                    ?: throw IllegalArgumentException(getString(R.string.Data_error))
                val assetPrice = extra.get("asset_price").asString
                    ?: throw IllegalArgumentException(getString(R.string.Data_error))
                val assetAmount = extra.get("asset_amount").asString
                    ?: throw IllegalArgumentException(getString(R.string.Data_error))
                this@OrderStatusFragment.binding.priceTv.text = "1 ${asset.symbol} = $assetPrice ${currency.name}"
                PriceExpiredBottomSheetDialogFragment.newInstance(
                    amount,
                    currency.name,
                    asset,
                    info.purchaseTotal,
                    assetAmount,
                    assetPrice,
                ).apply {
                    continueAction = { assetAmount ->
                        this@OrderStatusFragment.retry(sessionId, instrumentId, token, assetAmount)
                    }
                    cancelAction = {
                        this@OrderStatusFragment.view?.navigate(R.id.action_wallet_status_to_wallet)
                    }
                }.showNow(parentFragmentManager, PriceExpiredBottomSheetDialogFragment.TAG)
                return@launch
            }
            showError(
                requireContext().getMixinErrorStringByCode(
                    response.errorCode,
                    response.errorDescription,
                ),
            )
        }
    }

    private suspend fun getPaymentStatus(paymentId: String) {
        lifecycleScope.launch(defaultErrorHandler) {
            while (isActive) {
                val response = fiatMoneyViewModel.payment(paymentId)
                if (response.data?.status == RoutePaymentStatus.Captured.name) {
                    assetAmount = response.data!!.assetAmount
                    status = OrderStatus.SUCCESS
                    break
                } else if (response.data?.status == RoutePaymentStatus.Declined.name) {
                    showError(response.data?.reason)
                } else if (response.isSuccess) {
                    delay(REFRESH_INTERVAL)
                } else {
                    showError(
                        requireContext().getMixinErrorStringByCode(
                            response.errorCode,
                            response.errorDescription,
                        ),
                    )
                }
            }
        }
    }

    private val defaultErrorHandler = CoroutineExceptionHandler { _, error ->
        showError(error.localizedMessage)
    }

    private suspend fun createToken(tokenJson: String) {
        val tokenResponse = fiatMoneyViewModel.token(RouteTokenRequest(tokenJson))
        if (tokenResponse.isSuccess && tokenResponse.data != null) {
            val tokenData = requireNotNull(tokenResponse.data)
            if (tokenData.tokenFormat.equals(PAN_ONLY, true)) {
                createSession(tokenData.scheme, tokenData.token)
            } else if (tokenData.tokenFormat.equals(CRYPTOGRAM_3DS, true)) {
                payments(null, null, tokenData.token, expectancy)
            } else {
                showError(R.string.Unsupported_payment_method)
            }
        } else {
            ErrorHandler.handleMixinError(tokenResponse.errorCode, tokenResponse.errorDescription)
            showError(tokenResponse.errorDescription)
        }
    }

    private suspend fun createSession(scheme: String, token: String? = null) {
        val sessionResponse = fiatMoneyViewModel.createSession(RouteSessionRequest(token, currency.name, scheme.lowercase(), asset.assetId, amount, instrumentId))
        if (sessionResponse.isSuccess && sessionResponse.data != null) {
            val session = requireNotNull(sessionResponse.data)
            init3DS(session)
        } else {
            ErrorHandler.handleMixinError(sessionResponse.errorCode, sessionResponse.errorDescription)
            showError(sessionResponse.errorDescription)
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        lifecycleScope.launch(defaultErrorHandler) {
            val tokenJsonPayload = requireNotNull(paymentData.paymentMethodToken?.token)
            createToken(tokenJsonPayload)
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
                    showError(R.string.Cancel)
                }
            }
        }

    private fun handleError(statusCode: Int, message: String?) {
        Timber.e("Status code: $statusCode")
        showError(message)
    }

    private fun showError(message: String?) {
        if (!isAdded) return
        status = OrderStatus.FAILED
        binding.content.text = message
    }

    private fun showError(@StringRes errorRes: Int = R.string.Unknown) {
        showError(getString(errorRes))
    }

    enum class OrderStatus {
        INITIALIZED,
        PROCESSING,
        FAILED,
        SUCCESS,
    }
}

package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.CheckoutApiServiceFactory
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.ENVIRONMENT_3DS
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.CreateSessionRequest
import one.mixin.android.api.request.SessionStatus
import one.mixin.android.api.response.CheckoutPaymentStatus
import one.mixin.android.api.response.CreateSessionResponse
import one.mixin.android.databinding.FragmentOrderStatusBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.checkout.PaymentRequest
import timber.log.Timber
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

        fun newInstance(assetItem: AssetItem, currency: Currency) =
            OrderStatusFragment().withArgs {
                putParcelable(TransactionsFragment.ARGS_ASSET, assetItem)
                putParcelable(ARGS_CURRENCY, currency)
            }
    }

    private val binding by viewBinding(FragmentOrderStatusBinding::bind)
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

    private var status = OrderStatus.INITIALIZED
        private set(value) {
            if (field != value) {
                field = value
                if (value == OrderStatus.PROCESSING) {
                    processing()
                } else if (value == OrderStatus.SUCCESS) {
                    success()
                } else if (value == OrderStatus.FAILED) {
                    failed()
                }
            }
        }

    private fun success() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 0
        binding.topVa.displayedChild = 0
        binding.title.setText(R.string.Success)
        binding.content.text = getString(R.string.Success_desc, asset.symbol, asset.symbol)
    }

    private fun failed() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 1
        binding.topVa.displayedChild = 1
        binding.cancelTv.isVisible = true
        binding.title.setText(R.string.Failed)
    }

    private fun processing() {
        binding.bottomVa.isVisible = true
        binding.bottomVa.displayedChild = 2
        binding.topVa.displayedChild = 2
        binding.title.setText(R.string.Processing)
        binding.content.setText(R.string.Processing_desc)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
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
        val info = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_INFO,
                OrderInfo::class.java,
            ),
        )
        binding.apply {
            transparentMask.isVisible = false
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
                                putInt(ARGS_AMOUNT, amount)
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

            val logo = when {
                isGooglePay -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_google_pay_small)
                scheme == "mastercard" -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_mastercard)
                else -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_visa)
            }.also {
                it?.setBounds(0, 0, 28.dp, 14.dp)
            }
            payWith.setCompoundDrawables(logo, null, null, null)
            payWith.text = if (isGooglePay) {
                "Google Pay"
            } else {
                info.number
            }
            priceTv.text = info.price
            purchaseTv.text = info.purchase
            feeTv.text = info.fee
            totalTv.text = info.total
        }
        if (isGooglePay) {
            payWithGoogle()
        } else {
            payWithCheckout()
        }
    }

    private fun init3DS(sessionResponse: CreateSessionResponse) {
        val checkout3DS = Checkout3DSService(
            MixinApplication.appContext,
            ENVIRONMENT_3DS,
            Locale.US,
            null,
            Uri.parse("mixin://checkout"),
        )

        val authenticationParameters = AuthenticationParameters(
            sessionResponse.sessionId,
            sessionResponse.sessionSecret,
            sessionResponse.scheme,
        )

        checkout3DS.authenticate(authenticationParameters) { result: AuthenticationResult ->
            when (result.resultType) {
                ResultType.Completed -> {
                    lifecycleScope.launch {
                        while (true) {
                            val session = try {
                                walletViewModel.getSession(sessionResponse.sessionId)
                            } catch (e: Exception) {
                                showError(e.message)
                                return@launch
                            }
                            if (session.isSuccess) {
                                if (session.data?.status == SessionStatus.Approved.value) {
                                    payments(
                                        sessionId = sessionResponse.sessionId,
                                        sessionResponse.instrumentId,
                                    )
                                    break
                                } else if (session.data?.status != SessionStatus.Pending.value && session.data?.status != SessionStatus.Processing.value) {
                                    showError(session.data?.status ?: session.errorDescription)
                                    return@launch
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
            if (binding.transparentMask.isVisible) {
                // do noting
            } else if (status == OrderStatus.SUCCESS) {
                view?.navigate(R.id.action_wallet_status_to_wallet)
            } else if (status == OrderStatus.FAILED) {
                view?.navigate(
                    R.id.action_wallet_status_to_select,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                        putInt(ARGS_AMOUNT, amount)
                        putParcelable(ARGS_CURRENCY, currency)
                    },
                )
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun payWithCheckout() = lifecycleScope.launch {
        status = OrderStatus.PROCESSING
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
                            asset.assetId,
                            amount,
                            instrumentId,
                        ),
                    )
                },
                defaultExceptionHandle = {
                    ErrorHandler.handleError(it)
                    showError(it.message)
                },
                successBlock = { response ->
                    if (response.isSuccess) {
                        init3DS(response.data!!)
                    } else {
                        showError(requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription))
                    }
                },
            )
        }
    }

    private fun payWithGoogle() {
        status = OrderStatus.PROCESSING
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

    private fun payments(sessionId: String, instrumentId: String) = lifecycleScope.launch {
        try {
            val response = walletViewModel.payment(
                PaymentRequest(
                    asset.assetId,
                    Session.getAccountId()!!,
                    sessionId,
                    instrumentId,
                    amount.toLong(),
                    currency.name,
                ),
            )
            if (response.isSuccess) {
                binding.transparentMask.isVisible = false
                if (response.data?.status == CheckoutPaymentStatus.Captured.name) {
                    status = OrderStatus.SUCCESS
                } else {
                    val paymentId = response.data?.paymentId
                    if (paymentId == null) {
                        status = OrderStatus.FAILED
                        showError(response.errorDescription)
                    }
                    while (true) {
                        val payment = walletViewModel.payment(paymentId!!)
                        if (payment.data?.status == CheckoutPaymentStatus.Captured.name) {
                            status = OrderStatus.SUCCESS
                            break
                        }
                        delay(2000)
                    }
                }
            } else {
                status = OrderStatus.FAILED
                showError(requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription))
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
                                            asset.assetId,
                                            amount,
                                        ),
                                    )
                                },
                                defaultExceptionHandle = {
                                    ErrorHandler.handleError(it)
                                    showError(it.message)
                                },
                                successBlock = { response ->
                                    if (response.isSuccess) {
                                        init3DS(response.data!!)
                                    } else {
                                        showError(requireContext().getMixinErrorStringByCode(response.errorCode, response.errorDescription))
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
                    status = OrderStatus.FAILED
                    showError(R.string.Cancel)
                }
            }
        }

    private fun handleError(statusCode: Int, message: String?) {
        showError(message)
    }

    private fun showError(message: String?) {
        if (!isAdded) return
        status = OrderStatus.FAILED
        binding.transparentMask.isVisible = false
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

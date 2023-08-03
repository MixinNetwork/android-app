package one.mixin.android.ui.wallet.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.CheckoutApiServiceFactory
import com.checkout.api.CheckoutApiService
import com.checkout.base.model.Environment
import com.checkout.frames.api.PaymentFlowHandler
import com.checkout.frames.api.PaymentFormMediator
import com.checkout.frames.screen.paymentform.PaymentFormConfig
import com.checkout.frames.style.theme.paymentform.PaymentFormStyleProvider
import com.checkout.tokenization.model.GooglePayTokenRequest
import com.checkout.tokenization.model.TokenDetails
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.pay.PayClient
import com.google.android.gms.wallet.PaymentData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.CheckoutPayService
import one.mixin.android.api.request.PayTokenRequest
import one.mixin.android.api.request.TokenData
import one.mixin.android.ui.wallet.CustomPaymentFormTheme
import one.mixin.android.util.GsonHelper
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutActivity : ComponentActivity() {

    private val addToGoogleWalletRequestCode = 1000
    private val model: CheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProductScreen(
                viewModel = model,
                googlePayButtonOnClick = { requestPayment() },
            )
        }
    }

    private fun requestPayment() {
        // Disables the button to prevent multiple clicks.
        model.setGooglePayButtonClickable(false)

        val task = model.getLoadPaymentDataTask()

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

            // Re-enables the Google Pay payment button.
            model.setGooglePayButtonClickable(true)
        }
    }

    // Handle potential conflict from calling loadPaymentData
    private val resolvePaymentForResult =
        registerForActivityResult(StartIntentSenderForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                RESULT_OK ->
                    result.data?.let { intent ->
                        Timber.e(intent.getStringExtra("paymentMethodToken"))
                        Timber.e(intent.getStringExtra("com.google.android.gms.wallet.PaymentData"))
                        PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                    }

                RESULT_CANCELED -> {
                    // The user cancelled the payment attempt
                }
            }
        }

    @Inject
    lateinit var checkoutPayService: CheckoutPayService

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        try {
            val tokenJsonPayload = paymentData.paymentMethodToken?.token
            if (tokenJsonPayload != null) {
                model.checkoutSuccess()
                    Timber.e("Pay token $tokenJsonPayload")
                    // val tokenData = GsonHelper.customGson.fromJson(tokenJsonPayload, TokenData::class.java)
                    // Timber.e("${tokenData.signature} ${tokenData.signedMessage}")
                    CheckoutApiServiceFactory.create(
                        BuildConfig.CHCEKOUT_ID,
                        Environment.SANDBOX,
                        this@CheckoutActivity
                    ).createToken(
                        GooglePayTokenRequest(tokenJsonPayload, { tokenDetails ->
                            val result = Intent().apply {
                                putExtra("Token", tokenDetails.token)
                            }
                            setResult(Activity.RESULT_OK, result)
                            finish()
                        }, {
                            Timber.e("failure $it")
                        })
                    )
                    // try {
                    //     val response =
                    //         checkoutPayService.token(PayTokenRequest("googlepay", tokenData))
                    //     Timber.e("${response.token} ${response.type} ${response.expiresOn}")
                    //     val result = Intent().apply {
                    //         putExtra("Token", response.token)
                    //     }
                    //     setResult(Activity.RESULT_OK, result)
                    //     finish()
                    // } catch (e: Exception) {
                    //     Timber.e(e)
                    // }
            } else {
                model.checkoutFaild()
            }
        } catch (error: Exception) {
            Timber.e("Error", "Error: $error")
        }
    }

    private fun handleError(statusCode: Int, message: String?) {
        Timber.e("Google Pay API error", "Error code: $statusCode, Message: $message")
    }

    @Deprecated("Deprecated and in use by Google Pay")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == addToGoogleWalletRequestCode) {
            when (resultCode) {
                RESULT_OK ->
                    Toast
                        .makeText(
                            this,
                            getString(R.string.Success),
                            Toast.LENGTH_LONG,
                        )
                        .show()

                RESULT_CANCELED -> {
                    // Save canceled
                }

                PayClient.SavePassesResult.SAVE_ERROR -> data?.let { intentData ->
                    val apiErrorMessage =
                        intentData.getStringExtra(PayClient.EXTRA_API_ERROR_MESSAGE)
                    handleError(resultCode, apiErrorMessage)
                }

                else -> handleError(
                    CommonStatusCodes.INTERNAL_ERROR,
                    "Unexpected non API" +
                        " exception when trying to deliver the task result to an activity!",
                )
            }

            // Re-enables the Google Pay payment button.
        }
    }

    class PayContract : ActivityResultContract<String, Intent?>() {
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(context, CheckoutActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }
    }
}

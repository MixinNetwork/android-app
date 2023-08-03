package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.checkout.base.model.Environment
import com.checkout.frames.api.PaymentFlowHandler
import com.checkout.frames.api.PaymentFormMediator
import com.checkout.frames.screen.paymentform.PaymentFormConfig
import com.checkout.frames.style.theme.paymentform.PaymentFormStyleProvider
import com.checkout.threedsecure.model.ThreeDSRequest
import com.checkout.threedsecure.model.ThreeDSResult
import com.checkout.threedsecure.model.ThreeDSResultHandler
import com.checkout.tokenization.model.TokenDetails
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import timber.log.Timber

class PaymentFragment : Fragment() {

    private val paymentFlowHandler = object : PaymentFlowHandler {
        override fun onSubmit() {
            // form submit initiated; you can choose to display a loader here
        }

        override fun onSuccess(tokenDetails: TokenDetails) {
            Timber.e("token:${tokenDetails.token}")
            onSuccess?.invoke(tokenDetails.token)
        }

        override fun onFailure(errorMessage: String) {
            // token request error
        }

        override fun onBackPressed() {
            // the user decided to leave the payment page
            parentFragmentManager.beginTransaction().remove(this@PaymentFragment).commitNow()
        }
    }

    private val threeDSResultHandler: ThreeDSResultHandler = { threeDSResult: ThreeDSResult ->
        when (threeDSResult) {
            is ThreeDSResult.Success -> {
                threeDSResult.token
            }
            is ThreeDSResult.Failure -> {
            }
            is ThreeDSResult.Error -> {
                threeDSResult.error
            }
        }
    }

    private val paymentFormConfig = PaymentFormConfig(
        publicKey = BuildConfig.CHCEKOUT_ID, // set your public key
        context = MixinApplication.appContext, // set context
        environment = Environment.SANDBOX, // todo replace set the environment
        paymentFlowHandler = paymentFlowHandler, // set the callback
        style = PaymentFormStyleProvider.provide(
            CustomPaymentFormTheme.providePaymentFormTheme(),
        ), // set the style
        supportedCardSchemeList = emptyList(), // set supported card schemes, by default uses all schemes
    )
    private val paymentFormMediator = PaymentFormMediator(paymentFormConfig)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return paymentFormMediator.provideFragmentContent(this).apply {
            setBackgroundColor(0xFFF)
        }
    }

    private fun request3DS(view: View) {
        val request = ThreeDSRequest(
            container = view.findViewById(R.id.content), // Provide a ViewGroup container for 3DS WebView
            challengeUrl = "",                     // Provide a 3D Secure URL
            successUrl = "http://example.com/success",
            failureUrl = "http://example.com/failure",
            resultHandler = threeDSResultHandler
        )
        paymentFormMediator.handleThreeDS(request)
    }

    var onSuccess: ((String) -> Unit)? = null

    companion object {
        val TAG: String = "PaymentFragment"
    }
}

package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.checkout.frames.api.PaymentFlowHandler
import com.checkout.frames.api.PaymentFormMediator
import com.checkout.frames.screen.paymentform.PaymentFormConfig
import com.checkout.frames.style.theme.paymentform.PaymentFormStyleProvider
import com.checkout.tokenization.model.TokenDetails
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.CHECKOUT_ENVIRONMENT
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment
import timber.log.Timber

class PaymentFragment : BaseFragment() {

    private val paymentFlowHandler = object : PaymentFlowHandler {
        override fun onSubmit() {
            // form submit initiated; you can choose to display a loader here
        }

        override fun onSuccess(tokenDetails: TokenDetails) {
            Timber.e("token:${tokenDetails.token}")
            Timber.e("token:${tokenDetails.issuerCountry}")
            Timber.e("token:${tokenDetails.scheme}")
            onSuccess?.invoke(tokenDetails.token)
        }

        override fun onFailure(errorMessage: String) {
            // token request error
            onFailure?.invoke(errorMessage)
        }

        override fun onBackPressed() {
            // the user decided to leave the payment page
            Timber.e("onBackPressed")
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
                .remove(this@PaymentFragment).commitNow()
        }
    }

    private val paymentFormConfig = PaymentFormConfig(
        publicKey = BuildConfig.CHCEKOUT_ID, // set your public key
        context = MixinApplication.appContext, // set context
        environment = CHECKOUT_ENVIRONMENT, // todo replace set the environment
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
        return paymentFormMediator.provideFragmentContent(this)
    }

    var onSuccess: ((String) -> Unit)? = null
    var onFailure: ((String) -> Unit)? = null

    override fun onBackPressed(): Boolean {
        paymentFlowHandler.onBackPressed()
        return true
    }

    companion object {
        val TAG: String = "PaymentFragment"
    }
}

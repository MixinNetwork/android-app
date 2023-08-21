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
            onLoading?.invoke()
        }

        override fun onSuccess(tokenDetails: TokenDetails) {
            onSuccess?.invoke(tokenDetails.token, tokenDetails.scheme?.lowercase())
        }

        override fun onFailure(errorMessage: String) {
            // token request error
            onFailure?.invoke(errorMessage)
        }

        override fun onBackPressed() {
            // the user decided to leave the payment page
            onBack?.invoke()
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

    var onSuccess: ((String, String?) -> Unit)? = null
    var onFailure: ((String) -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var onLoading: (() -> Unit)? = null

    override fun onBackPressed(): Boolean {
        paymentFlowHandler.onBackPressed()
        return true
    }

    companion object {
        const val TAG: String = "PaymentFragment"
    }
}

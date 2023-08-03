package one.mixin.android.ui.wallet.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import one.mixin.android.BuildConfig
import one.mixin.android.ui.wallet.PaymentsUtil
import timber.log.Timber

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    data class State(
        val googlePayAvailable: Boolean? = false,
        val googleWalletAvailable: Boolean? = false,
        val googlePayButtonClickable: Boolean = true,
        val checkoutSuccess: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // A client for interacting with the Google Pay API.
    private val paymentsClient: PaymentsClient = PaymentsUtil.createPaymentsClient(application)

    init {
        fetchCanUseGooglePay()
    }

    /**
     * Determine the user's ability to pay with a payment method supported by your app and display
     * a Google Pay payment button.
     ) */
    private fun fetchCanUseGooglePay() {
        val isReadyToPayJson = PaymentsUtil.isReadyToPayRequest()
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
        val task = paymentsClient.isReadyToPay(request)

        task.addOnCompleteListener { completedTask ->
            try {
                _state.update { currentState ->
                    currentState.copy(googlePayAvailable = completedTask.getResult(ApiException::class.java))
                }
            } catch (exception: ApiException) {
                Timber.w("isReadyToPay failed", exception)
            }
        }
    }

    fun getLoadPaymentDataTask(): Task<PaymentData> {
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
            .addParameter("gatewayMerchantId", BuildConfig.CHCEKOUT_ID)
            .build()
        request.setPaymentMethodTokenizationParameters(params)
        return paymentsClient.loadPaymentData(request.build())
    }

    /**
     * Determine whether the API to save passes to Google Pay is available on the device.
     */

    fun setGooglePayButtonClickable(clickable: Boolean) {
        _state.update { currentState ->
            currentState.copy(googlePayButtonClickable = clickable)
        }
    }

    fun checkoutSuccess() {
        _state.update { currentState ->
            currentState.copy(checkoutSuccess = true)
        }
    }

    fun checkoutFaild() {
        // Todo
    }
}

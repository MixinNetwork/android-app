package one.mixin.android.ui.wallet.fiatmoney

import android.os.Parcelable
import androidx.lifecycle.ViewModel
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.api.request.RouteSessionRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.request.RouteTokenRequest
import one.mixin.android.api.response.RouteCreateTokenResponse
import one.mixin.android.api.response.RoutePaymentResponse
import one.mixin.android.api.response.RouteSessionResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.PaymentsUtil
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Card
import one.mixin.android.vo.SafeBox
import one.mixin.android.vo.route.RoutePaymentRequest
import one.mixin.android.vo.sumsub.RouteTokenResponse
import retrofit2.Call
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FiatMoneyViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    suspend fun findAssetsByIds(ids: List<String>) = assetRepository.findAssetsByIds(ids)

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        assetRepository.ticker(tickerRequest)

    suspend fun token(): MixinResponse<RouteTokenResponse> = assetRepository.token()

    fun callSumsubToken(): Call<MixinResponse<RouteTokenResponse>> = assetRepository.callSumsubToken()

    suspend fun payment(traceRequest: RoutePaymentRequest): MixinResponse<RoutePaymentResponse> = assetRepository.payment(traceRequest)

    suspend fun payment(paymentId: String): MixinResponse<RoutePaymentResponse> = assetRepository.payment(paymentId)

    suspend fun payments(): MixinResponse<List<RoutePaymentResponse>> = assetRepository.payments()

    suspend fun createSession(createSession: RouteSessionRequest): MixinResponse<RouteSessionResponse> = assetRepository.createSession(createSession)

    suspend fun token(createSession: RouteTokenRequest): MixinResponse<RouteCreateTokenResponse> = assetRepository.token(createSession)

    suspend fun createInstrument(createInstrument: RouteInstrumentRequest): MixinResponse<Card> = assetRepository.createInstrument(createInstrument)

    suspend fun getSession(sessionId: String) = assetRepository.getSession(sessionId)

    fun cards(): Flow<SafeBox?> = assetRepository.cards()
    suspend fun initSafeBox(cards: List<Card>) = assetRepository.initSafeBox(cards)

    suspend fun instruments() = assetRepository.instruments()

    suspend fun addCard(card: Card) = assetRepository.addCard(card)

    suspend fun removeCard(index: Int) = assetRepository.removeCard(index)

    suspend fun deleteInstruments(id: String) = assetRepository.deleteInstruments(id)

    var calculateState: CalculateState? = null

    var asset: AssetItem? = null
    var currency: Currency? = null

    @Parcelize
    class CalculateState(
        var minimum: Int = 15,
        var maximum: Int = 1000,
        var assetPrice: Float = 1f,
    ) : Parcelable

    var isReverse: Boolean = false

    data class State(
        val googlePayAvailable: Boolean? = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val paymentsClient: PaymentsClient by lazy {
        PaymentsUtil.createPaymentsClient(MixinApplication.appContext)
    }

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

    fun getLoadPaymentDataTask(totalPrice: String, currencyCode: String): Task<PaymentData> {
        val request = PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice(totalPrice)
                    .setCurrencyCode(currencyCode)
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
            .addParameter("gateway", Constants.RouteConfig.PAYMENTS_GATEWAY)
            .addParameter("gatewayMerchantId", BuildConfig.CHCEKOUT_ID)
            .build()
        request.setPaymentMethodTokenizationParameters(params)
        return paymentsClient.loadPaymentData(request.build())
    }
}

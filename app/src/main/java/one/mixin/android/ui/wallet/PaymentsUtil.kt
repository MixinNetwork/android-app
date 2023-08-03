package one.mixin.android.ui.wallet

import android.content.Context
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import one.mixin.android.Constants
import one.mixin.android.Constants.PAYMENTS_ENVIRONMENT
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object PaymentsUtil {

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)

    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {
            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
                put(
                    "billingAddressParameters",
                    JSONObject().apply {
                        put("format", "FULL")
                    },
                )
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    fun isReadyToPayRequest(): JSONObject? {
        return try {
            baseRequest.apply {
                put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
            }
        } catch (e: JSONException) {
            null
        }
    }

    fun createPaymentsClient(context: Context): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(PAYMENTS_ENVIRONMENT)
            .build()

        return Wallet.getPaymentsClient(context, walletOptions)
    }
}

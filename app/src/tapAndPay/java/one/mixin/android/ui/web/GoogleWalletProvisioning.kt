package one.mixin.android.ui.web

import android.app.Activity
import android.content.Intent
import android.util.Base64
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest

object GoogleWalletProvisioning {
    private const val requestCode = 8294

    fun isAvailable(): Boolean = true

    fun start(activity: Activity, request: GoogleWalletProvisioningRequest): Boolean =
        runCatching {
            TapAndPay
                .getClient(activity)
                .pushTokenize(
                    activity,
                    PushTokenizeRequest
                        .Builder()
                        .setOpaquePaymentCard(Base64.decode(request.opaquePaymentCard, Base64.DEFAULT))
                        .setNetwork(TapAndPay.CARD_NETWORK_VISA)
                        .setTokenServiceProvider(TapAndPay.TOKEN_PROVIDER_VISA)
                        .setDisplayName(request.displayName)
                        .setLastDigits(request.lastDigits)
                        .build(),
                    requestCode,
                )
        }.isSuccess

    fun resultFor(requestCode: Int, resultCode: Int, data: Intent?): GoogleWalletProvisioningResult? {
        if (requestCode != GoogleWalletProvisioning.requestCode) return null
        return when (resultCode) {
            Activity.RESULT_OK -> GoogleWalletProvisioningResult.Success
            Activity.RESULT_CANCELED -> GoogleWalletProvisioningResult.Cancelled
            else -> GoogleWalletProvisioningResult.Error
        }
    }
}

package one.mixin.android.ui.web

import android.app.Activity
import android.content.Intent

object GoogleWalletProvisioning {
    fun isAvailable(): Boolean = false

    fun start(_activity: Activity, _request: GoogleWalletProvisioningRequest): Boolean = false

    fun resultFor(_requestCode: Int, _resultCode: Int, _data: Intent?): GoogleWalletProvisioningResult? = null
}

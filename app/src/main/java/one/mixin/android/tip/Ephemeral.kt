package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.service.TipService
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import javax.inject.Inject

// manage Ephemeral
class Ephemeral @Inject internal constructor (private val tipService: TipService) {

    suspend fun getEphemeralSeed(context: Context, deviceId: String): String {
        val ephemeralSeed = context.defaultSharedPreferences.getString(Constants.Tip.Ephemeral_Seed, null)
        if (!ephemeralSeed.isNullOrEmpty()) {
            return ephemeralSeed
        }
        val response = tipService.tipEphemerals()
        if (!response.isSuccess) {
            return ""
        }
        if (response.data.isNullOrEmpty()) {
            return ""
        }
        val ep = response.data!![0]
        val resp = tipService.tipEphemeral(TipRequest(deviceId, ep.seedBase64))
        if (!resp.isSuccess) {
            return ""
        }
        context.defaultSharedPreferences.putString(Constants.Tip.Ephemeral_Seed, ep.seedBase64)
        return ep.seedBase64
    }
}
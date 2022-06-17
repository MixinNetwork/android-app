package one.mixin.android.tip

import android.content.Context
import one.mixin.android.Constants
import one.mixin.android.api.request.TipRequest
import one.mixin.android.api.service.TipService
import one.mixin.android.crypto.generateEphemeralSeed
import one.mixin.android.extension.base64RawEncode
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import javax.inject.Inject

// manage Ephemeral
class Ephemeral @Inject internal constructor(private val tipService: TipService) {

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
            return createEphemeralSeed(context, deviceId)
        }
        val ep = response.data!![0]
        return updateEphemeralSeed(context, deviceId, ep.seedBase64)
    }

    private suspend fun createEphemeralSeed(context: Context, deviceId: String): String {
        val seed = generateEphemeralSeed().base64RawEncode()
        return updateEphemeralSeed(context, deviceId, seed)
    }

    private suspend fun updateEphemeralSeed(context: Context, deviceId: String, seed: String): String {
        val resp = tipService.tipEphemeral(TipRequest(deviceId, seed))
        if (!resp.isSuccess) {
            return ""
        }
        context.defaultSharedPreferences.putString(Constants.Tip.Ephemeral_Seed, seed)
        return seed
    }
}

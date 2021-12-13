package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.api.response.SignalKeyCount
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SignalKeyService {

    @POST("signal/keys")
    suspend fun pushSignalKeys(@Body signalKeyRequest: SignalKeyRequest): MixinResponse<String?>

    @GET("signal/keys/count")
    fun getSignalKeyCount(): Call<MixinResponse<SignalKeyCount>>
}

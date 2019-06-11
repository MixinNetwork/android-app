package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.response.VerificationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyService {

    @POST("emergency_verifications")
    suspend fun create(@Body request: EmergencyRequest): MixinResponse<VerificationResponse>

    @POST("emergency_verifications/{id}")
    suspend fun verify(@Path("id") id: String, @Body request: EmergencyRequest): MixinResponse<VerificationResponse>

    @GET("emergency_contact")
    suspend fun show(): MixinResponse<VerificationResponse>
}
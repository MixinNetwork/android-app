package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyService {
    @POST("emergency_verifications")
    suspend fun create(
        @Body request: EmergencyRequest,
    ): MixinResponse<VerificationResponse>

    @POST("emergency_verifications/{id}")
    suspend fun createVerify(
        @Path("id") id: String,
        @Body request: EmergencyRequest,
    ): MixinResponse<Account>

    @POST("emergency_verifications/{id}")
    suspend fun loginVerify(
        @Path("id") id: String,
        @Body request: EmergencyRequest,
    ): MixinResponse<Account>

    @POST("emergency_contact")
    suspend fun show(
        @Body pin: PinRequest,
    ): MixinResponse<User>

    @POST("emergency_contact/delete")
    suspend fun delete(
        @Body pin: PinRequest,
    ): MixinResponse<Account>
}

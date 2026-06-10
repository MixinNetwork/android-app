package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.response.ProvisioningResponse
import one.mixin.android.api.response.ProvisioningResponseCode
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProvisioningService {
    @GET("device/provisioning/code")
    suspend fun provisionCodeAsync(): MixinResponse<ProvisioningResponseCode>

    @POST("provisionings/{id}")
    suspend fun updateProvisioningAsync(
        @Path("id") id: String,
        @Body request: ProvisioningRequest,
    ): MixinResponse<ProvisioningResponse>
}

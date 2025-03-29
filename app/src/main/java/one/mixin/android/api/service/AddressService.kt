package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AddressRequest
import one.mixin.android.api.request.Pin
import one.mixin.android.vo.Address
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressService {
    @POST("addresses")
    suspend fun addresses(
        @Body request: AddressRequest,
    ): MixinResponse<Address>

    @POST("addresses/{id}/delete")
    suspend fun delete(
        @Path("id") id: String,
        @Body pin: Pin,
    ): MixinResponse<Unit>

    @GET("addresses/{id}")
    suspend fun address(
        @Path("id") id: String,
    ): MixinResponse<Address>
}

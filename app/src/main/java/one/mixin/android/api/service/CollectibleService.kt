package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.NonFungibleToken
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CollectibleService {
    @GET("collectible/tokens/{id}")
    suspend fun tokens(@Path("id") id: String): MixinResponse<NonFungibleToken>

    @GET("collectible/outputs")
    suspend fun outputs(@Path("id") id: String): MixinResponse<NonFungibleToken>

    @POST("collectible/requests")
    suspend fun create(@Path("id") id: String, @Body request: CollectibleRequest): MixinResponse<NonFungibleToken>

    @POST("collectible/requests/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body request: CollectibleRequest): MixinResponse<NonFungibleToken>

    @POST("collectible/requests/{id}/sign")
    suspend fun sign(@Path("id") id: String, @Body request: CollectibleRequest): MixinResponse<NonFungibleToken>

    @POST("collectible/requests/{id}/unlock")
    suspend fun unlock(@Path("id") id: String, @Body request: CollectibleRequest): MixinResponse<NonFungibleToken>
}

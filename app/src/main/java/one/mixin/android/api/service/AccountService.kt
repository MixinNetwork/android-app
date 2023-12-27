package one.mixin.android.api.service

import com.google.gson.JsonObject
import io.reactivex.Observable
import kotlinx.coroutines.Deferred
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.LogoutRequest
import one.mixin.android.api.request.NonFungibleToken
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.DeviceCheckResponse
import one.mixin.android.api.response.SessionSecretResponse
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.vo.Account
import one.mixin.android.vo.Fiat
import one.mixin.android.vo.LogResponse
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.TurnServer
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AccountService {
    @POST("verifications")
    fun verificationObserver(
        @Body request: VerificationRequest,
    ): Observable<MixinResponse<VerificationResponse>>

    @POST("verifications")
    suspend fun verification(
        @Body request: VerificationRequest,
    ): MixinResponse<VerificationResponse>

    @POST("verifications/{id}")
    suspend fun create(
        @Path("id") id: String,
        @Body request: AccountRequest,
    ): MixinResponse<Account>

    @POST("verifications/{id}")
    suspend fun changePhone(
        @Path("id") id: String,
        @Body request: AccountRequest,
    ): MixinResponse<Account>

    @POST("verifications/{id}")
    fun deactiveVerification(
        @Path("id") id: String,
        @Body request: DeactivateVerificationRequest,
    ): Observable<MixinResponse<VerificationResponse>>

    @POST("me")
    fun update(
        @Body request: AccountUpdateRequest,
    ): Observable<MixinResponse<Account>>

    @POST("devices")
    fun deviceCheck(): Observable<MixinResponse<DeviceCheckResponse>>

    @POST("me/preferences")
    suspend fun preferences(
        @Body request: AccountUpdateRequest,
    ): MixinResponse<Account>

    @GET("safe/me")
    fun getMe(): Call<MixinResponse<Account>>

    @GET("safe/me")
    suspend fun getMeSuspend(): MixinResponse<Account>

    @POST("me/deactivate")
    suspend fun deactivate(
        @Body request: DeactivateRequest,
    ): MixinResponse<Account>

    @POST("logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): MixinResponse<Unit>

    @GET("codes/{id}")
    suspend fun code(
        @Path("id") id: String,
    ): MixinResponse<JsonObject>

    @POST("pin/update")
    suspend fun updatePinSuspend(
        @Body request: PinRequest,
    ): MixinResponse<Account>

    @POST("pin/verify")
    suspend fun verifyPin(
        @Body request: PinRequest,
    ): MixinResponse<Account>

    @POST("session")
    fun updateSession(
        @Body request: SessionRequest,
    ): Observable<MixinResponse<Account>>

    @GET("stickers/albums")
    suspend fun getStickerAlbums(): MixinResponse<List<StickerAlbum>>

    @GET("stickers/albums/{id}")
    fun getStickersByAlbumId(
        @Path("id") id: String,
    ): Call<MixinResponse<List<Sticker>>>

    @GET("stickers/albums/{id}")
    suspend fun getStickersByAlbumIdSuspend(
        @Path("id") id: String,
    ): MixinResponse<List<Sticker>>

    @GET("albums/{id}")
    suspend fun getAlbumByIdSuspend(
        @Path("id") id: String,
    ): MixinResponse<StickerAlbum>

    @GET("stickers/{id}")
    fun getStickerById(
        @Path("id") id: String,
    ): Call<MixinResponse<Sticker>>

    @GET("stickers/{id}")
    suspend fun getStickerByIdSuspend(
        @Path("id") id: String,
    ): MixinResponse<Sticker>

    @POST("stickers/favorite/add")
    fun addStickerAsync(
        @Body request: StickerAddRequest,
    ): Deferred<MixinResponse<Sticker>>

    @POST("stickers/favorite/remove")
    fun removeSticker(
        @Body ids: List<String>,
    ): Call<MixinResponse<Sticker>>

    @GET("turn")
    suspend fun getTurn(): MixinResponse<Array<TurnServer>>

    @GET("/")
    fun ping(): Call<MixinResponse<Void>>

    @GET("external/fiats")
    suspend fun getFiats(): MixinResponse<List<Fiat>>

    @GET("logs")
    suspend fun getPinLogs(
        @Query("category") category: String? = null,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int? = null,
    ): MixinResponse<List<LogResponse>>

    @POST("multisigs/{id}/cancel")
    suspend fun cancelMultisigs(
        @Path("id") id: String,
    ): MixinResponse<Void>

    @POST("multisigs/{id}/sign")
    suspend fun signMultisigs(
        @Path("id") id: String,
        @Body pinRequest: PinRequest,
    ): MixinResponse<Void>

    @POST("multisigs/{id}/unlock")
    suspend fun unlockMultisigs(
        @Path("id") id: String,
        @Body pinRequest: PinRequest,
    ): MixinResponse<Void>

    @GET("collectibles/tokens/{id}")
    suspend fun getToken(
        @Path("id") id: String,
    ): MixinResponse<NonFungibleToken>

    @POST("collectibles/requests/{id}/cancel")
    suspend fun cancelCollectibleTransfer(
        @Path("id") id: String,
    ): MixinResponse<NonFungibleToken>

    @POST("collectibles/requests/{id}/sign")
    suspend fun signCollectibleTransfer(
        @Path("id") id: String,
        @Body request: CollectibleRequest,
    ): MixinResponse<NonFungibleToken>

    @POST("collectibles/requests/{id}/unlock")
    suspend fun unlockCollectibleTransfer(
        @Path("id") id: String,
        @Body request: CollectibleRequest,
    ): MixinResponse<NonFungibleToken>

    @POST("transactions")
    suspend fun transactions(
        @Body request: RawTransactionsRequest,
    ): MixinResponse<Void>

    @POST("session/secret")
    suspend fun modifySessionSecret(
        @Body request: SessionSecretRequest,
    ): MixinResponse<SessionSecretResponse>

    @GET("external/schemes")
    suspend fun getExternalSchemes(): MixinResponse<Set<String>>

    @GET("external/addresses/check")
    suspend fun validateExternalAddress(
        @Query("asset") assetId: String,
        @Query("destination") destination: String,
        @Query("tag") tag: String?,
    ): MixinResponse<AddressResponse>
}

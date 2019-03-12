package one.mixin.android.api.service

import com.google.gson.JsonObject
import io.reactivex.Observable
import kotlinx.coroutines.Deferred
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.vo.Account
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.TurnServer
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AccountService {

    @POST("verifications")
    fun verification(@Body request: VerificationRequest): Observable<MixinResponse<VerificationResponse>>

    @POST("verifications/{id}")
    fun create(@Path("id") id: String, @Body request: AccountRequest): Observable<MixinResponse<Account>>

    @POST("verifications/{id}")
    fun changePhone(@Path("id") id: String, @Body request: AccountRequest): Observable<MixinResponse<Account>>

    @POST("me")
    fun update(@Body request: AccountUpdateRequest): Observable<MixinResponse<Account>>

    @POST("me/preferences")
    fun preferences(@Body request: AccountUpdateRequest): Observable<MixinResponse<Account>>

    @GET("me")
    fun getMe(): Call<MixinResponse<Account>>

    @POST("logout")
    fun logoutAsync(): Deferred<MixinResponse<Unit>>

    @GET("codes/{id}")
    fun code(@Path("id") id: String): Observable<MixinResponse<JsonObject>>

    @POST("invitations/{code}")
    fun invitations(@Path("code") code: String): Observable<MixinResponse<Account>>

    @POST("pin/update")
    fun updatePin(@Body request: PinRequest): Observable<MixinResponse<Account>>

    @POST("pin/verify")
    fun verifyPin(@Body request: PinRequest): Observable<MixinResponse<Account>>

    @POST("session")
    fun updateSession(@Body request: SessionRequest): Observable<MixinResponse<Account>>

    @GET("stickers/albums")
    fun getStickerAlbums(): Call<MixinResponse<List<StickerAlbum>>>

    @GET("stickers/albums/{id}")
    fun getStickersByAlbumId(@Path("id") id: String): Call<MixinResponse<List<Sticker>>>

    @GET("stickers/{id}")
    fun getStickerById(@Path("id") id: String): Call<MixinResponse<Sticker>>

    @POST("stickers/favorite/add")
    fun addSticker(@Body request: StickerAddRequest): Observable<MixinResponse<Sticker>>

    @POST("stickers/favorite/remove")
    fun removeSticker(@Body ids: List<String>): Call<MixinResponse<Sticker>>

    @GET("turn")
    fun getTurn(): Observable<MixinResponse<Array<TurnServer>>>

    @GET("/")
    fun ping(): Call<MixinResponse<Void>>
}
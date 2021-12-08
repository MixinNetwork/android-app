package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.response.AuthorizationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthorizationService {

    @POST("oauth/authorize")
    fun authorize(@Body request: AuthorizeRequest): Observable<MixinResponse<AuthorizationResponse>>

    @GET("authorizations")
    fun authorizations(): Observable<MixinResponse<List<AuthorizationResponse>>>

    @GET("authorizations")
    suspend fun getAuthorizationByAppId(@Query("app") appId: String): MixinResponse<List<AuthorizationResponse>>

    @POST("oauth/cancel")
    fun deAuthorize(@Body request: DeauthorRequest): Observable<MixinResponse<Map<String, String?>>>
}

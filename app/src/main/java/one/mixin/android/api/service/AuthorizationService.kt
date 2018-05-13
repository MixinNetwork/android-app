package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.response.AuthorizationResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthorizationService {

    @POST("oauth/authorize")
    fun authorize(@Body request: AuthorizeRequest): Observable<MixinResponse<AuthorizationResponse>>
}
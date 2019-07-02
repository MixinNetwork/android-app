package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.vo.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserService {

    @POST("users/fetch")
    fun getUsers(@Body ids: List<String>): Call<MixinResponse<List<User>>>

    @GET("users/{id}")
    fun getUserById(@Path("id") id: String): Call<MixinResponse<User>>

    @GET("search/{query}")
    fun search(@Path("query") query: String): Observable<MixinResponse<User>>

    @POST("relationships")
    fun relationship(@Body request: RelationshipRequest): Observable<MixinResponse<User>>

    @POST("reports")
    fun report(@Body request: RelationshipRequest): Observable<MixinResponse<User>>

    @GET("blocking_users")
    fun blockingUsers(): Observable<MixinResponse<List<User>>>
}
package one.mixin.android.api.service

import io.reactivex.Observable
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.response.UserSession
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
    suspend fun relationship(@Body request: RelationshipRequest): MixinResponse<User>

    @POST("reports")
    suspend fun report(@Body request: RelationshipRequest): MixinResponse<User>

    @GET("blocking_users")
    fun blockingUsers(): Observable<MixinResponse<List<User>>>

    @POST("users/fetch")
    suspend fun fetchUsers(@Body ids: List<String>): MixinResponse<List<User>>

    @POST("sessions/fetch")
    suspend fun fetchSessions(@Body ids: List<String>): MixinResponse<List<UserSession>>
}

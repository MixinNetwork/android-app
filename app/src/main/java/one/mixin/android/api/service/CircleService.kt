package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.CircleConversationRequest
import one.mixin.android.job.RefreshCircleJob.Companion.REFRESH_CIRCLE_CONVERSATION_LIMIT
import one.mixin.android.vo.Circle
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.CircleName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CircleService {
    @GET("circles")
    fun getCircles(): Call<MixinResponse<List<Circle>>>

    @GET("circles/{id}")
    fun getCircle(@Path("id") id: String): Call<MixinResponse<Circle>>

    @POST("circles")
    suspend fun createCircle(@Body body: CircleName): MixinResponse<Circle>

    @POST("circles/{id}")
    suspend fun updateCircle(@Path("id") id: String, @Body body: CircleName): MixinResponse<Circle>

    @POST("circles/{id}/delete")
    suspend fun deleteCircle(@Path("id") id: String): MixinResponse<Map<String, String?>?>

    @POST("circles/{id}/conversations")
    suspend fun updateCircleConversations(
        @Path("id") id: String,
        @Body conversationCircleRequests: List<CircleConversationRequest>
    ): MixinResponse<List<CircleConversation>>

    @GET("circles/{id}/conversations")
    fun getCircleConversations(
        @Path("id") id: String,
        @Query("offset") offset: String? = null,
        @Query("limit") limit: Int = REFRESH_CIRCLE_CONVERSATION_LIMIT
    ): Call<MixinResponse<List<CircleConversation>>>
}

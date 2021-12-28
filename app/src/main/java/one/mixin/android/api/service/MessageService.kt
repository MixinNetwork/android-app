package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MessageService {
    @GET("messages/status/{offset}")
    fun messageStatusOffset(@Path("offset") offset: Long): Call<MixinResponse<List<BlazeMessageData>>>

    @POST("acknowledgements")
    suspend fun acknowledgements(@Body ack: List<BlazeAckMessage>): MixinResponse<Map<String, String?>?>
}

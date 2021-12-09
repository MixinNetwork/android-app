package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.ContactRequest
import one.mixin.android.vo.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ContactService {

    @POST("contacts")
    suspend fun syncContacts(@Body contacts: List<ContactRequest>): MixinResponse<Map<String, String?>>

    @GET("friends")
    fun friends(): Call<MixinResponse<List<User>>>

    @GET("contacts")
    suspend fun contacts(): MixinResponse<List<User>>
}

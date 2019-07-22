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
    fun syncContacts(@Body contacts: List<ContactRequest>): Call<MixinResponse<Any>>

    @GET("friends")
    fun friends(): Call<MixinResponse<List<User>>>

    @GET("contacts")
    fun contacts(): Call<MixinResponse<List<User>>>
}

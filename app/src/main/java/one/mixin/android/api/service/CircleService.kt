package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.vo.Circle
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CircleService {
    @GET("circles")
    fun getCircles(): Call<MixinResponse<List<Circle>>>

    @POST("circles")
    fun createCircle(): MixinResponse<Any>

    @POST("/circles/{id}")
    fun updateCircle(@Path("id") id: String): MixinResponse<Any>

    @POST("/circles/{id}/delete")
    fun deleteCircle(@Path("id") id: String): MixinResponse<Any>

    @POST("/circles/{id}/conversations")
    fun getCircle(@Path("id") id: String): MixinResponse<Any>
}

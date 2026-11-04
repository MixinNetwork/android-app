package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.vo.giphy.SearchData
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyService {
    @GET("gifs/trending")
    suspend fun trendingGifs(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("api_key") apiKey: String = BuildConfig.GIPHY_KEY,
    ): SearchData

    @GET("gifs/search")
    suspend fun searchGifs(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("api_key") apiKey: String = BuildConfig.GIPHY_KEY,
    ): SearchData
}

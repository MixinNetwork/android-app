package one.mixin.android.api.service

import one.mixin.android.BuildConfig
import one.mixin.android.vo.foursquare.FoursquareResult
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface FoursquareService {

    @GET("venues/search")
    suspend fun searchVenues(
        @Query("ll") latlng: String,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("v") date: String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()),
        @Query("client_id") clientId: String = BuildConfig.FS_CLIENT_ID,
        @Query("client_secret") clientSecret: String = BuildConfig.FS_SECRET
    ): FoursquareResult
}

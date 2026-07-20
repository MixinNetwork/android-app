package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.referral.ReferralResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ReferralService {
    @GET("referral")
    suspend fun referral(): MixinResponse<ReferralResponse>

    @GET("app-banners")
    suspend fun walletHomeBanners(
        @Query("chain") chains: List<String>? = null,
    ): MixinResponse<List<WalletHomeBanner>>
}

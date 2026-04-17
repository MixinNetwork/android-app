package one.mixin.android.api.service

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.referral.ReferralResponse
import retrofit2.http.GET

interface ReferralService {
    @GET("referral")
    suspend fun referral(): MixinResponse<ReferralResponse>
}

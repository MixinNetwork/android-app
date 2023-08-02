package one.mixin.android.api

import one.mixin.android.vo.sumsub.TokenRequest
import one.mixin.android.vo.sumsub.TokenResponse
import retrofit2.http.POST
import retrofit2.http.Url

interface SumsubService {

    @POST
    suspend fun token(request: TokenRequest, @Url url: String = "https://wallet.touge.fun/kyc/token"): TokenResponse
}

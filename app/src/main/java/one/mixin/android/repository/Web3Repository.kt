package one.mixin.android.repository

import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.web3.EstimateFeeRequest
import one.mixin.android.api.request.web3.EstimateFeeResponse
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.Web3Service
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.Account
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.safe.TokensExtra
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

@Singleton
class Web3Repository
@Inject
constructor(
    val routeService: RouteService,
    val webService: Web3Service,
) {
    suspend fun estimateFee(request: EstimateFeeRequest) = routeService.estimateFee(request)

    suspend fun transactions(
        address: String,
        chainId: String,
        fungibleId: String,
        assetKey: String,
        limit: Int = 100,
    ) = webService.transactions(address, chainId, fungibleId, assetKey, limit)

    suspend fun web3Account(account: String) = webService.web3Account(account)

    suspend fun web3Tokens(chain: String, addresses: String?) =
        webService.web3Tokens(chain, addresses)
}

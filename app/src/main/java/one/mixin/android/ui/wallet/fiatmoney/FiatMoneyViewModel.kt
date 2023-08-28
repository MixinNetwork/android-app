package one.mixin.android.ui.wallet.fiatmoney

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.setting.Currency
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.sumsub.RouteTokenResponse
import one.mixin.android.vo.sumsub.UserResponse
import javax.inject.Inject

@HiltViewModel
class FiatMoneyViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val assetRepository: AssetRepository,
) : ViewModel() {
    suspend fun findAssetsByIds(ids: List<String>) = assetRepository.findAssetsByIds(ids)

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        assetRepository.ticker(tickerRequest)

    suspend fun token(): MixinResponse<RouteTokenResponse> = assetRepository.token()

    suspend fun getUser(id: String): MixinResponse<UserResponse> = assetRepository.getUser(id)

    var state: CalculateState? = null

    var asset: AssetItem? = null
    var currency: Currency? = null

    class CalculateState(
        var minimum: Int = 15,
        var maximum: Int = 1000,
        var fiatPrice: Float = 1f,
    )

    var isReverse: Boolean = false
}

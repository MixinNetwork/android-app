@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet.fiatmoney

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RampWebUrlRequest
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.RampWebUrlResponse
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.Currency
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.sumsub.ProfileResponse
import javax.inject.Inject

@HiltViewModel
class FiatMoneyViewModel
    @Inject
    internal constructor(
        private val userRepository: UserRepository,
        private val tokenRepository: TokenRepository,
    ) : ViewModel() {
        suspend fun findAssetsByIds(ids: List<String>) = tokenRepository.findAssetsByIds(ids)

        suspend fun findAssetsById(id: String) = tokenRepository.findAssetItemById(id)

        suspend fun findOrSyncApp(id: String) = userRepository.findOrSyncApp(id)

        suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)

        suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
            tokenRepository.ticker(tickerRequest)

        var calculateState: CalculateState? = null

        var asset: TokenItem? = null
        var currency: Currency? = null

        @Parcelize
        class CalculateState(
            var minimum: Int = 15,
            var assetPrice: Float = 1f,
            var feePercent: Float = 0f,
        ) : Parcelable

        var isReverse: Boolean = false

        suspend fun profile(): MixinResponse<ProfileResponse> = tokenRepository.profile()

        suspend fun syncNoExistAsset(assetIds: List<String>) =
            withContext(Dispatchers.IO) {
                assetIds.forEach { id ->
                    if (tokenRepository.findAssetItemById(id) == null) {
                        tokenRepository.findOrSyncAsset(id)
                    }
                }
            }

        suspend fun rampWebUrl(
            amount: String,
            assetId: String,
            currency: String,
            destination: String
        ): MixinResponse<RampWebUrlResponse> =
            tokenRepository.rampWebUrl(
                RampWebUrlRequest(
                    amount = amount,
                    assetId = assetId,
                    currency = currency,
                    destination = destination,
                    phone = Session.getAccount()?.phone,
                    phoneVerifiedAt = Session.getAccount()?.phoneVerifiedAt,
                )
            )

        suspend fun findAndSyncDepositEntry(chainId: String, assetId: String) = withContext(Dispatchers.IO) {
            tokenRepository.findAndSyncDepositEntry(chainId, assetId)
        }
    }

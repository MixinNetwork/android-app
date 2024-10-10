package one.mixin.android.ui.wallet.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.maxAlertsPerAsset
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.maxTotalAlerts
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertStatus
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import javax.inject.Inject

@HiltViewModel
class AlertViewModel
@Inject
internal constructor(val tokenRepository: TokenRepository) : ViewModel() {
    suspend fun add(alert: AlertRequest) = tokenRepository.addAlert(alert)

    fun alertGroups() = tokenRepository.alertGroups()

    fun alertGroups(coinIds: List<String>) = tokenRepository.alertGroups(coinIds)

    fun alertGroup(coinId: String) = tokenRepository.alertGroup(coinId)

    fun alertsByCoinId(coinId: String) = tokenRepository.alertsByCoinId(coinId)

    suspend fun simpleCoinItem(coinId: String) = tokenRepository.simpleCoinItem(coinId)

    suspend fun updateAlert(alertId: String, request: AlertUpdateRequest): MixinResponse<Unit>? {
        val r = tokenRepository.updateAlert(alertId, "update", request)
        if (r?.isSuccess == true) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenRepository.updateAlert(alertId, request.type, request.value, request.frequency)
            }
        }
        return r
    }

    private fun getTotalAlertCount(): Int = tokenRepository.getTotalAlertCount()

    private fun getAlertCountByCoinId(coinId: String): Int = tokenRepository.getAlertCountByCoinId(coinId)

    fun isTotalAlertCountExceeded() = getTotalAlertCount() >= maxTotalAlerts

    fun isAssetAlertCountExceeded(coinId: String) = getAlertCountByCoinId(coinId) >= maxAlertsPerAsset

    suspend fun updateAlert(alertId: String, action: AlertAction) {
        val r = tokenRepository.updateAlert(
            alertId, action = when (action) {
                AlertAction.DELETE -> "delete"
                AlertAction.RESUME -> "resume"
                AlertAction.PAUSE -> "pause"
                AlertAction.EDIT -> ""
            }
        )
        if (r?.isSuccess == true) {
            when (action) {
                AlertAction.DELETE -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.deleteAlertById(alertId)
                    }
                }

                AlertAction.RESUME -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.updateAlertStatus(alertId, AlertStatus.RUNNING)
                    }
                }

                AlertAction.PAUSE -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.updateAlertStatus(alertId, AlertStatus.PAUSED)
                    }
                }

                AlertAction.EDIT -> {}
            }
        }
    }
}

package one.mixin.android.ui.wallet.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

@HiltViewModel
class AlertViewModel
@Inject
internal constructor(val tokenRepository: TokenRepository) : ViewModel() {
    suspend fun add(alert: AlertRequest) = tokenRepository.addAlert(alert)

    suspend fun requestAlerts() = tokenRepository.requestAlerts()

    fun alertGroups() = tokenRepository.alertGroups()

    fun alertGroups(assetIds: List<String>) = tokenRepository.alertGroups(assetIds)

    fun alertsByAssetId(assetId: String) = tokenRepository.alertsByAssetId(assetId)

    suspend fun simpleAssetItem(assetId: String) = tokenRepository.simpleAssetItem(assetId)

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

    private fun getAlertCountByAssetId(assetId: String): Int = tokenRepository.getAlertCountByAssetId(assetId)

    fun checkCount(assetId: String): Int {
        return if (getTotalAlertCount() >= maxTotalAlerts) maxTotalAlerts
        else if (getAlertCountByAssetId(assetId) >= maxAlertsPerAsset) maxAlertsPerAsset
        else 0
    }

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

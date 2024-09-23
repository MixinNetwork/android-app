package one.mixin.android.ui.wallet.alert

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.wallet.alert.vo.AlertActionRquest
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import javax.inject.Inject

@HiltViewModel
class AlertViewModel
@Inject
internal constructor(val tokenRepository: TokenRepository) : ViewModel() {
    suspend fun add(alert: AlertRequest) = tokenRepository.addAlert(alert)

    suspend fun requestAlerts() = tokenRepository.requestAlerts()

    suspend fun updateAlert(alert: AlertActionRquest) = tokenRepository.updateAlert(alert)

    fun alertGroups() = tokenRepository.alertGroups()

    fun alertGroups(assetIds:List<String>) = tokenRepository.alertGroups(assetIds)

    fun alertsByAssetId(assetId: String) = tokenRepository.alertsByAssetId(assetId)

    suspend fun simpleAssetItem(assetId: String) = tokenRepository.simpleAssetItem(assetId)
}
package one.mixin.android.ui.wallet.alert

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.TokenRepository
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import javax.inject.Inject

@HiltViewModel
class AlertViewModel
@Inject
internal constructor(val tokenRepository: TokenRepository) : ViewModel() {
    suspend fun add(alert: AlertRequest) = tokenRepository.addAlert(alert)

    suspend fun requestAlerts() = tokenRepository.requestAlerts()

    fun alertGroups() = tokenRepository.alertGroups()

    fun alertsByAssetId(assetId: String) = tokenRepository.alertsByAssetId(assetId)
}
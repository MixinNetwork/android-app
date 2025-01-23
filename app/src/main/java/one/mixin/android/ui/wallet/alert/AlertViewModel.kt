package one.mixin.android.ui.wallet.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.MIXIN_ALERT_USER_ID
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.TokenRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.maxAlertsPerAsset
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.maxTotalAlerts
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertAction
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest

@HiltViewModel
class AlertViewModel
@Inject internal constructor(private val jobManager: MixinJobManager, val userRepository: UserRepository, val tokenRepository: TokenRepository) : ViewModel() {
    suspend fun add(alert: AlertRequest): MixinResponse<Alert>? {
        val r = tokenRepository.addAlert(alert)
        if (r?.isSuccess == true) {
            addAlertBot()
        }
        return r
    }

    fun alertGroups() = tokenRepository.alertGroups()

    fun alertGroups(coinIds: List<String>) = tokenRepository.alertGroups(coinIds)

    fun alertGroup(coinId: String) = tokenRepository.alertGroup(coinId)

    fun alertsByCoinId(coinId: String) = tokenRepository.alertsByCoinId(coinId)

    suspend fun simpleCoinItem(coinId: String) = tokenRepository.simpleCoinItem(coinId)

    suspend fun updateAlert(alertId: String, request: AlertUpdateRequest): MixinResponse<Alert>? {
        val r = tokenRepository.updateAlert(alertId, request)
        if (r?.isSuccess == true) {
            viewModelScope.launch(Dispatchers.IO) {
                addAlertBot()
                tokenRepository.insertAlert(r.data!!)
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
            alertId, AlertUpdateRequest(
                action = when (action) {
                    AlertAction.DELETE -> "delete"
                    AlertAction.RESUME -> "resume"
                    AlertAction.PAUSE -> "pause"
                    AlertAction.EDIT -> ""
                }
            )
        )
        if (r?.isSuccess == true) {
            addAlertBot()
            when (action) {
                AlertAction.DELETE -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.deleteAlertById(alertId)
                    }
                }

                AlertAction.RESUME -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.insertAlert(r.data!!)
                    }
                }

                AlertAction.PAUSE -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        tokenRepository.insertAlert(r.data!!)
                    }
                }

                AlertAction.EDIT -> {}
            }
        }
    }

    private fun addAlertBot() {
        viewModelScope.launch(Dispatchers.IO) {
            val bot = userRepository.getUserById(MIXIN_ALERT_USER_ID)
            if (bot == null || bot.relationship != "FRIEND") {
                jobManager.addJobInBackground(UpdateRelationshipJob(RelationshipRequest(MIXIN_ALERT_USER_ID, RelationshipAction.ADD.name)))
            }
        }
    }
}

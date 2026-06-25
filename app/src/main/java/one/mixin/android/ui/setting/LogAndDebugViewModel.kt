package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.repository.TokenRepository
import one.mixin.android.session.Session
import one.mixin.android.util.retrieveFirebaseMessagingToken
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LogAndDebugViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val accountService: AccountService,
) : ViewModel() {

    suspend fun deleteAllWeb3Transactions() {
        tokenRepository.deleteAllWeb3Transactions()
    }

    suspend fun deleteWallets() {
        tokenRepository.deleteWallets()
    }

    suspend fun deleteAllOrders() {
        tokenRepository.deleteAllOrders()
    }

    suspend fun updateFcmToken(): FcmTokenUpdateResult {
        Timber.e("Debug FCM token update started")
        if (Session.getAccount() == null) {
            Timber.w("Debug FCM token update failed: No active account")
            return FcmTokenUpdateResult.Failure("No active account")
        }

        val token = try {
            retrieveFirebaseMessagingToken()
        } catch (e: Exception) {
            Timber.e(e, "Debug FCM token retrieval failed")
            return FcmTokenUpdateResult.Failure("Failed to retrieve Firebase token: ${e.displayMessage()}")
        }
        if (token.isBlank()) {
            Timber.e("Debug FCM token retrieval failed: blank token")
            return FcmTokenUpdateResult.Failure("Firebase token is blank")
        }
        Timber.e("Debug Firebase token retrieved: true")

        return try {
            val response = accountService.updateSession(SessionRequest(notificationToken = token))
            if (response.isSuccess) {
                Timber.e("Debug session updated successfully with Firebase token")
                FcmTokenUpdateResult.Success
            } else {
                val message = buildString {
                    append("Session update failed with error code: ${response.errorCode}")
                    if (response.errorDescription.isNotBlank()) {
                        append('\n')
                        append(response.errorDescription)
                    }
                }
                Timber.e(message)
                FcmTokenUpdateResult.Failure(message)
            }
        } catch (e: Exception) {
            Timber.e(e, "Debug session update failed")
            FcmTokenUpdateResult.Failure("Session update failed: ${e.displayMessage()}")
        }
    }

    private fun Exception.displayMessage() = message ?: javaClass.simpleName
}

sealed interface FcmTokenUpdateResult {
    object Success : FcmTokenUpdateResult

    data class Failure(val message: String) : FcmTokenUpdateResult
}

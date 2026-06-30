package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler.Companion.SERVER
import one.mixin.android.util.reportFcmException
import one.mixin.android.util.retrieveFirebaseMessagingToken
import timber.log.Timber

@HiltWorker
class SessionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
) : BaseWork(context, parameters) {

    override suspend fun onRun(): Result {
        Timber.e("SessionWorker started")
        val account = Session.getAccount()
        if (account == null) {
            Timber.w("Session update failed: No active account")
            return Result.failure()
        }

        val token = if (applicationContext.isGooglePlayServicesAvailable()) {
            try {
                retrieveFirebaseToken()
            } catch (e: Exception) {
                Timber.e(e, "Failed to retrieve Firebase token")
                reportFcmException("SessionWorker failed to retrieve Firebase token", e)
                null
            }
        } else {
            Timber.w("Google Play services unavailable, skipping Firebase token retrieval")
            null
        }
        val notificationToken = if (token != null && token.isBlank()) {
            val error = IllegalStateException("SessionWorker retrieved blank Firebase token")
            Timber.e(error, "Failed to retrieve Firebase token")
            reportFcmException("SessionWorker retrieved blank Firebase token", error)
            null
        } else {
            token
        }
        Timber.e("Firebase token retrieved: ${notificationToken != null}")

        return try {
            val response = accountService.updateSession(SessionRequest(notificationToken = notificationToken))
            if (response.isSuccess) {
                Timber.e("Session updated successfully")
                Result.success()
            } else if (response.errorCode >= SERVER) {
                Timber.e("Session update failed with server error, retrying...")
                Result.retry()
            } else {
                Timber.e("Session update failed with error code: ${response.errorCode}")
                Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating session")
            Result.retry()
        }
    }

    private suspend fun retrieveFirebaseToken(): String = retrieveFirebaseMessagingToken()
}

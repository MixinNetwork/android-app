package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler.Companion.SERVER
import one.mixin.android.util.reportException
import timber.log.Timber

@HiltWorker
class SessionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
) : BaseWork(context, parameters) {
    private val appContext = context.applicationContext

    override suspend fun onRun(): Result {
        Timber.e("SessionWorker started")
        val account = Session.getAccount()
        if (account == null) {
            Timber.w("Session update failed: No active account")
            return Result.failure()
        }
        if (!appContext.isGooglePlayServicesAvailable()) {
            Timber.w("Session update skipped: Google Play services unavailable")
            return Result.success()
        }

        val token = try {
            retrieveFirebaseToken()
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve Firebase token, retrying session update")
            reportException(IllegalStateException("SessionWorker failed to retrieve Firebase token", e))
            return Result.retry()
        }
        if (token.isBlank()) {
            val error = IllegalStateException("SessionWorker retrieved blank Firebase token")
            Timber.e(error, "Failed to retrieve Firebase token, retrying session update")
            reportException(error)
            return Result.retry()
        }
        Timber.e("Firebase token retrieved: true")

        return try {
            val response = accountService.updateSession(SessionRequest(notificationToken = token))
            if (response.isSuccess) {
                Timber.e("Session updated successfully with Firebase token")
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

    private suspend fun retrieveFirebaseToken(): String = FirebaseMessaging.getInstance().token.await()
}

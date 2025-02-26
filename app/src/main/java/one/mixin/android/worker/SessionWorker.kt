package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.session.Session
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
        
        val token = retrieveFirebaseToken()
        Timber.e("Firebase token retrieved: ${token != null}")
        
        return try {
            val response = accountService.updateSession(SessionRequest(notificationToken = token))
            if (response.isSuccess) {
                Timber.e("Session updated successfully")
                Result.success()
            } else {
                Timber.e("Session update failed with error code: ${response.errorCode}")
                Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating session")
            Result.retry()
        }
    }
    
    private fun retrieveFirebaseToken(): String? {
        return runCatching {
            Tasks.await(FirebaseMessaging.getInstance().token)
        }.onFailure { error ->
            Timber.e(error, "Failed to retrieve Firebase token")
        }.getOrDefault(null)
    }
}
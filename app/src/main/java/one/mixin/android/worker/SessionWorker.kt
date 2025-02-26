package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import one.mixin.android.repository.UserRepository
import one.mixin.android.session.Session
import one.mixin.android.vo.Account
import timber.log.Timber

@HiltWorker
class SessionWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
) : BaseWork(context, parameters) {
    override suspend fun onRun(): Result {
        Timber.e("SessionWorker.onRun() started")
        if (Session.getAccount() == null) {
            Timber.e("SessionWorker.onRun() account is null, returning failure")
            return Result.failure()
        }
        val token = runCatching {
            Tasks.await(FirebaseMessaging.getInstance().token)
        }.onFailure {
            Timber.e(it)
        }.getOrDefault(null)
        Timber.e("SessionWorker.onRun() Firebase token: $token")
        Timber.e("SessionWorker.onRun() calling updateSession")
        val response = updateSession(SessionRequest(notificationToken = token))
        Timber.e("SessionWorker.onRun() updateSession response: ${response.isSuccess}")
        return if (response.isSuccess) {
            Result.success()
        } else {
            Timber.e("SessionWorker.onRun() updateSession response: ${response.errorCode}")
            Result.failure()
        }
    }

    private suspend fun updateSession(request: SessionRequest): MixinResponse<Account> {
        return accountService.updateSession(request)
    }
}
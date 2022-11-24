package one.mixin.android.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService

@HiltWorker
class RefreshFcmWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    val accountService: AccountService,
) : BaseWork(context, parameters) {

    companion object {
        const val TOKEN = "token"
    }

    @SuppressLint("CheckResult")
    override suspend fun onRun(): Result {
        val token = inputData.getString(TOKEN)
        if (token != null) {
            accountService.updateSession(SessionRequest(notificationToken = token))
                .observeOn(Schedulers.io()).subscribe({}, {})
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                accountService.updateSession(SessionRequest(notificationToken = it))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }
        }
        return Result.success()
    }
}

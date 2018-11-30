package one.mixin.android.worker

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.WorkerParameters
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.service.AccountService
import javax.inject.Inject

class RefreshFcmWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    @Inject
    lateinit var accountService: AccountService

    companion object {
        const val TOKEN = "token"
    }

    @SuppressLint("CheckResult")
    override fun onRun(): Result {
        val token = inputData.getString(TOKEN)
        if (token != null) {
            accountService.updateSession(SessionRequest(notificationToken = token))
                .observeOn(Schedulers.io()).subscribe({}, {})
        } else {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
                accountService.updateSession(SessionRequest(notificationToken = result.token))
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }
        }
        return Result.SUCCESS
    }
}
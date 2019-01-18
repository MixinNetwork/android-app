package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.api.service.ContactService
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.User

class RefreshContactWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val contactService: ContactService,
    private val userRepo: UserRepository
) : BaseWork(context, parameters) {

    override fun onRun(): Result {
        val response = contactService.friends().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val users = response.data as List<User>
            users.forEach {
                if (it.app != null) {
                    it.appId = it.app!!.appId
                    userRepo.insertApp(it.app!!)
                }
                userRepo.upsert(it)
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}
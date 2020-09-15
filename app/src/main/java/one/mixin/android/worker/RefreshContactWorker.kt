package one.mixin.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import one.mixin.android.api.service.ContactService
import one.mixin.android.db.AppDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdateList
import one.mixin.android.vo.User

class RefreshContactWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val contactService: ContactService,
    private val userDao: UserDao,
    private val appDao: AppDao
) : BaseWork(context, parameters) {

    override suspend fun onRun(): Result {
        val response = contactService.friends().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val users = response.data as List<User>
            userDao.insertUpdateList(users, appDao)
            Result.success()
        } else {
            Result.failure()
        }
    }
}

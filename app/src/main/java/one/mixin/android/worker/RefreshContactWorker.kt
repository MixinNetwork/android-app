package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.service.ContactService
import one.mixin.android.db.AppDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdateList
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.vo.User

class RefreshContactWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val contactService: ContactService,
    private val userDao: UserDao,
    private val appDao: AppDao
) : BaseWork(context, parameters) {

    override fun onRun(): Result {
        val response = contactService.friends().execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val users = response.data as List<User>
            runBlocking {
                userDao.insertUpdateList(users, appDao)
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}

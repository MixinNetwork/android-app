package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.UserService
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.extension.enqueueAvatarWorkRequest
import one.mixin.android.repository.UserRepository
import one.mixin.android.worker.AvatarWorker.Companion.GROUP_ID

class RefreshUserWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val userService: UserService,
    private val userRepo: UserRepository
) : BaseWork(context, parameters) {

    companion object {
        const val USER_IDS = "user_ids"
        const val CONVERSATION_ID = "conversation_id"
    }

    override suspend fun onRun(): Result {
        val userIds = inputData.getStringArray(USER_IDS) ?: return Result.failure()
        val conversationId = inputData.getString(CONVERSATION_ID)
        val call = userService.getUsers(userIds.toList()).execute()
        val response = call.body()
        return if (response != null && response.isSuccess) {
            response.data?.let { data ->
                userRepo.upsertList(data)

                conversationId?.let {
                    WorkManager.getInstance(MixinApplication.appContext).enqueueAvatarWorkRequest(
                        workDataOf(GROUP_ID to conversationId)
                    )
                }
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}

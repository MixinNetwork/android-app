package one.mixin.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import one.mixin.android.MixinApplication
import one.mixin.android.api.service.UserService
import one.mixin.android.extension.buildNetworkRequest
import one.mixin.android.extension.buildRequest
import one.mixin.android.repository.UserRepository
import one.mixin.android.worker.AvatarWorker.Companion.GROUP_ID

class RefreshUserWorker @WorkerInject constructor(
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
                    WorkManager.getInstance(MixinApplication.appContext)
                        .beginWith(buildNetworkRequest<DownloadAvatarWorker>(workDataOf(GROUP_ID to conversationId)).build())
                        .then(buildRequest<GenerateAvatarWorker>(workDataOf(GROUP_ID to conversationId)).build())
                        .enqueue()
                }
            }
            Result.success()
        } else {
            Result.failure()
        }
    }
}

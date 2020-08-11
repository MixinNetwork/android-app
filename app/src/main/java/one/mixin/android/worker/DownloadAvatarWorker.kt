package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.vo.User
import java.util.concurrent.TimeUnit

class DownloadAvatarWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters
) : AvatarWorker(context, parameters) {

    override suspend fun onRun(): Result {
        val groupId = inputData.getString(GROUP_ID) ?: return Result.failure()
        val triple = checkGroupAvatar(groupId)
        if (triple.first) {
            return Result.success()
        }
        try {
            downloadBitmaps(users)
        } catch (e: Exception) {
            return Result.success()
        }
        return Result.success()
    }

    private fun downloadBitmaps(users: MutableList<User>) {
        for (i in 0 until users.size) {
            val item = users[i].avatarUrl
            if (!item.isNullOrEmpty()) {
                Glide.with(applicationContext)
                    .asBitmap()
                    .load(item)
                    .submit()
                    .get(20, TimeUnit.SECONDS)
            }
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}

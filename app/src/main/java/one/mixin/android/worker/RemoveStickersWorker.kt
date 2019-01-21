package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.di.worker.ChildWorkerFactory

class RemoveStickersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
    private val stickerRelationshipDao: StickerRelationshipDao
) : BaseWork(context, parameters) {

    companion object {
        const val STICKER_IDS = "sticker_ids"
    }

    override fun onRun(): Result {
        val stickerIds = inputData.getStringArray(STICKER_IDS)?.toList()
        if (stickerIds.isNullOrEmpty()) return Result.failure()
        for (i in stickerIds) {
            stickerRelationshipDao.deleteByStickerId(i)
        }
        accountService.removeSticker(stickerIds).execute()
        return Result.success()
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}
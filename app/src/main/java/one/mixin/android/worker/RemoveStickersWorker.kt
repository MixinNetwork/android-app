package one.mixin.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerRelationshipDao

class RemoveStickersWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
    private val stickerRelationshipDao: StickerRelationshipDao
) : BaseWork(context, parameters) {

    companion object {
        const val STICKER_IDS = "sticker_ids"
    }

    override suspend fun onRun(): Result {
        val stickerIds = inputData.getStringArray(STICKER_IDS)?.toList()
        if (stickerIds.isNullOrEmpty()) return Result.failure()
        stickerRelationshipDao.getPersonalAlbumId()?.let { albumId ->
            for (i in stickerIds) {
                stickerRelationshipDao.deleteByStickerId(i, albumId)
            }
        }
        accountService.removeSticker(stickerIds).execute()
        return Result.success()
    }
}

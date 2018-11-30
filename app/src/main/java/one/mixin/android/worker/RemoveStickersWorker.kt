package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerRelationshipDao
import javax.inject.Inject

class RemoveStickersWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    companion object {
        const val STICKER_IDS = "sticker_ids"
    }

    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var stickerRelationshipDao: StickerRelationshipDao

    override fun onRun(): Result {
        val stickerIds = inputData.getStringArray(STICKER_IDS)?.toList()
        if (stickerIds.isNullOrEmpty()) return Result.FAILURE
        for (i in stickerIds) {
            stickerRelationshipDao.deleteByStickerId(i)
        }
        accountService.removeSticker(stickerIds).execute()
        return Result.SUCCESS
    }
}
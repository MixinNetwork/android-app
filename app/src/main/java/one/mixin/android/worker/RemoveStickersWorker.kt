package one.mixin.android.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.di.worker.AndroidWorkerInjector
import javax.inject.Inject

class RemoveStickersWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    companion object {
        const val STICKER_IDS = "sticker_ids"
    }

    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var stickerRelationshipDao: StickerRelationshipDao

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val stickerIds = inputData.getStringArray(STICKER_IDS)?.toList()
        if (stickerIds.isNullOrEmpty()) return Result.FAILURE
        return try {
            for (i in stickerIds) {
                stickerRelationshipDao.deleteByStickerId(i)
            }
            accountService.removeSticker(stickerIds).execute()
            Result.SUCCESS
        } catch (e: Exception) {
            Result.FAILURE
        }
    }
}
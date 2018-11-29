package one.mixin.android.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.vo.Sticker
import javax.inject.Inject

class RefreshStickerWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    companion object {
        const val STICKER_ID = "sticker_id"
    }

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var stickerDao: StickerDao

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val stickerId = inputData.getString(STICKER_ID) ?: return Result.FAILURE
        return try {
            val response = accountService.getStickerById(stickerId).execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val s = response.data as Sticker
                stickerDao.insertUpdate(s)
                try {
                    Glide.with(applicationContext).load(s.assetUrl).submit(s.assetWidth, s.assetHeight)
                } catch (e: Exception) {
                }
                Result.SUCCESS
            } else {
                Result.FAILURE
            }
        } catch (e: Exception) {
            return Result.FAILURE
        }
    }
}
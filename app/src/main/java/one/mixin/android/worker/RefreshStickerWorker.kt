package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.vo.Sticker
import javax.inject.Inject

class RefreshStickerWorker(context: Context, parameters: WorkerParameters) : BaseWork(context, parameters) {

    companion object {
        const val STICKER_ID = "sticker_id"
    }

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var stickerDao: StickerDao

    override fun onRun(): Result {
        val stickerId = inputData.getString(STICKER_ID) ?: return Result.FAILURE
        val response = accountService.getStickerById(stickerId).execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
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
    }
}
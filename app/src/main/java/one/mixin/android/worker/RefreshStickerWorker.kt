package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.vo.Sticker

class RefreshStickerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
    private val stickerDao: StickerDao
) : BaseWork(context, parameters) {

    companion object {
        const val STICKER_ID = "sticker_id"
    }

    override fun onRun(): Result {
        val stickerId = inputData.getString(STICKER_ID) ?: return Result.failure()
        val response = accountService.getStickerById(stickerId).execute().body()
        return if (response != null && response.isSuccess && response.data != null) {
            val s = response.data as Sticker
            runBlocking {
                stickerDao.insertUpdate(s)
            }
            try {
                Glide.with(applicationContext).load(s.assetUrl).submit(s.assetWidth, s.assetHeight)
            } catch (e: Exception) {
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}

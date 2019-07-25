package one.mixin.android.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.di.worker.ChildWorkerFactory
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship

class RefreshStickerAlbumWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
    private val stickerAlbumDao: StickerAlbumDao,
    private val stickerDao: StickerDao,
    private val stickerRelationshipDao: StickerRelationshipDao
) : BaseWork(context, parameters) {

    override fun onRun(): Result {
        val response = accountService.getStickerAlbums().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val albums = response.data as List<StickerAlbum>
            for (a in albums) {
                stickerAlbumDao.insert(a)

                val r = accountService.getStickersByAlbumId(a.albumId).execute().body()
                if (r != null && r.isSuccess && r.data != null) {
                    val stickers = r.data as List<Sticker>
                    for (s in stickers) {
                        runBlocking {
                            stickerDao.insertUpdate(s) {
                                stickerRelationshipDao.insert(StickerRelationship(a.albumId, s.stickerId))
                            }
                        }
                    }
                }
            }
            val sp = applicationContext.defaultSharedPreferences
            if (!sp.getBoolean("UpgradeMessageSticker", false)) {
                stickerRelationshipDao.updateMessageStickerId()
                sp.putBoolean("UpgradeMessageSticker", true)
            }
            return Result.success()
        } else {
            return Result.failure()
        }
    }

    @AssistedInject.Factory
    interface Factory : ChildWorkerFactory
}

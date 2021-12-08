package one.mixin.android.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship

@HiltWorker
class RefreshStickerAlbumWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val accountService: AccountService,
    private val stickerAlbumDao: StickerAlbumDao,
    private val stickerDao: StickerDao,
    private val stickerRelationshipDao: StickerRelationshipDao
) : BaseWork(context, parameters) {

    override suspend fun onRun(): Result {
        val response = accountService.getStickerAlbums()
        if (response.isSuccess && response.data != null) {
            val albums = response.data as List<StickerAlbum>
            for (a in albums) {
                stickerAlbumDao.insert(a)

                val r = accountService.getStickersByAlbumId(a.albumId).execute().body()
                if (r != null && r.isSuccess && r.data != null) {
                    val stickers = r.data as List<Sticker>
                    for (s in stickers) {
                        stickerDao.insertUpdate(s)
                        stickerRelationshipDao.insert(StickerRelationship(a.albumId, s.stickerId))
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
}

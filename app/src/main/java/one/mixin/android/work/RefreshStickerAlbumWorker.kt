package one.mixin.android.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.di.worker.AndroidWorkerInjector
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship
import javax.inject.Inject

class RefreshStickerAlbumWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var stickerAlbumDao: StickerAlbumDao

    @Inject
    lateinit var stickerDao: StickerDao

    @Inject
    lateinit var stickerRelationshipDao: StickerRelationshipDao

    override fun doWork(): Result {
        AndroidWorkerInjector.inject(this)
        val response = accountService.getStickerAlbums().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
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
        }
        return Result.SUCCESS
    }
}
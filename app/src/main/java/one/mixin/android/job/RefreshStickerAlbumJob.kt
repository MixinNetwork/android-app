package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.insertUpdate
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship

class RefreshStickerAlbumJob : BaseJob(Params(PRIORITY_UI_HIGH)
    .addTags(RefreshStickerAlbumJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerAlbumJob"
    }

    override fun onRun() {
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
        }
    }
}
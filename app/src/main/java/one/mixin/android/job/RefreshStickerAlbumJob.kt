package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship

class RefreshStickerAlbumJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(RefreshStickerAlbumJob.GROUP).requireNetwork()
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerAlbumJob"
        const val REFRESH_STICKER_ALBUM_PRE_KEY = "refresh_sticker_album_pre_key"
    }

    override fun onRun() {
        val response = accountService.getStickerAlbums().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val albums = response.data as List<StickerAlbum>
            stickerAlbumDao.insertList(albums)
            for (a in albums) {
                val r = accountService.getStickersByAlbumId(a.albumId).execute().body()
                if (r != null && r.isSuccess && r.data != null) {
                    val stickers = r.data as List<Sticker>
                    val relationships = arrayListOf<StickerRelationship>()
                    for (s in stickers) {
                        stickerDao.insertUpdate(s)
                        relationships.add(StickerRelationship(a.albumId, s.stickerId))
                    }
                    stickerRelationshipDao.insertList(relationships)
                }
            }
            val sp = applicationContext.defaultSharedPreferences
            if (!sp.getBoolean("UpgradeMessageSticker", false)) {
                stickerRelationshipDao.updateMessageStickerId()
                sp.putBoolean("UpgradeMessageSticker", true)
            }
        }
    }
}

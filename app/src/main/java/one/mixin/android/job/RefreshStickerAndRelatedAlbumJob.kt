package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerRelationship

class RefreshStickerAndRelatedAlbumJob(private val stickerId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerAndRelatedAlbumJob"
    }

    override fun onRun() =
        runBlocking {
            val localSticker = stickerDao.findStickerById(stickerId)
            var albumId: String? = null
            if (localSticker != null) {
                albumId = localSticker.albumId
            }

            if (albumId.isNullOrBlank()) {
                val response = accountService.getStickerById(stickerId).execute().body()
                if (response != null && response.isSuccess && response.data != null) {
                    albumId = (response.data as Sticker).albumId
                }
            }

            if (albumId.isNullOrBlank()) {
                albumId?.let { stickerDao.updateAlbumId(stickerId, it) }
                return@runBlocking
            }

            var album = stickerAlbumDao.findAlbumById(albumId)
            if (album == null) {
                val albumResponse = accountService.getAlbumByIdSuspend(albumId)
                if (albumResponse.isSuccess && albumResponse.data != null) {
                    album = albumResponse.data as StickerAlbum
                    stickerAlbumDao.insertSuspend(album)
                }
            }

            if (album == null || album.category == "PERSONAL") {
                stickerDao.updateAlbumId(stickerId, albumId)
                return@runBlocking
            }

            val stickersResponse = accountService.getStickersByAlbumIdSuspend(albumId)
            if (stickersResponse.isSuccess && stickersResponse.data != null) {
                val stickers = stickersResponse.data as List<Sticker>
                val relationships: MutableList<StickerRelationship>? =
                    if (album.category == "SYSTEM") {
                        arrayListOf()
                    } else {
                        null
                    }
                relationships?.addAll(stickers.map { StickerRelationship(albumId, it.stickerId) })
                stickers.forEach {
                    stickerDao.insertUpdate(it)
                }
                relationships?.let { rs ->
                    stickerRelationshipDao.insertList(rs)
                }
            }
        }
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.insertUpdate
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerRelationship

class RefreshAlbumStickersJob(
    private val albumId: String,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork()
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAlbumStickersJob"
    }

    override fun onRun() = runBlocking {
        val r = accountService.getStickersByAlbumIdSuspend(albumId)
        if (r.isSuccess && r.data != null) {
            val stickers = r.data as List<Sticker>
            val relationships = arrayListOf<StickerRelationship>()
            for (s in stickers) {
                stickerDao.insertUpdate(s)
                relationships.add(StickerRelationship(albumId, s.stickerId))
            }
            stickerRelationshipDao.insertList(relationships)
        }
    }
}

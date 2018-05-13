package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.Sticker

class RefreshStickerJob(private val albumId: String) : BaseJob(Params(PRIORITY_UI_HIGH)
    .addTags(RefreshStickerJob.GROUP).persist().requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerJob"
    }

    override fun onRun() {
        val response = accountService.getStickers(albumId).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val stickers = response.data as List<Sticker>
            for (s in stickers) {
                val sticker = stickerDao.getStickerByUnique(s.albumId, s.name)
                if (sticker != null) {
                    s.lastUseAt = sticker.lastUseAt
                }
                stickerDao.insert(s)
            }
        }
    }
}
package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.StickerAlbum

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

                jobManager.addJobInBackground(RefreshStickerJob(a.albumId))
            }
        }
    }
}
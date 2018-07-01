package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import one.mixin.android.db.insertUpdate
import one.mixin.android.vo.Sticker

class RefreshStickerJob(private val stickerId: String) : BaseJob(Params(PRIORITY_UI_HIGH)
    .addTags(RefreshStickerJob.GROUP).persist().requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshStickerJob"
    }

    override fun onRun() {
        val response = accountService.getStickerById(stickerId).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val s = response.data as Sticker
            stickerDao.insertUpdate(s)
            try {
                Glide.with(applicationContext).load(s.assetUrl).downloadOnly(s.assetWidth, s.assetHeight)
            } catch (e: Exception) {
            }
        }
    }
}
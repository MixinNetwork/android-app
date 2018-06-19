package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class RemoveStickersJob(private val stickerIds: List<String>)
    : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RemoveStickersJob"
    }

    override fun onRun() {
        if (stickerIds.isEmpty()) return

        for (i in stickerIds) {
            stickerDao.deleteByStickerId(i)
        }
        val response = accountService.removeSticker(stickerIds).execute().body()
        if (response != null && response.isSuccess) {

        }
    }
}
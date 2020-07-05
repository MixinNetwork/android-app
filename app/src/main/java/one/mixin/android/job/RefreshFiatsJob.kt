package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.vo.Fiats

class RefreshFiatsJob : BaseJob(
    Params(PRIORITY_UI_HIGH).addTags(GROUP).persist().requireNetwork()
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshFiatsJob"
    }

    override fun onRun() = runBlocking {
        val resp = accountService.getFiats()
        if (resp.isSuccess) {
            resp.data?.let { fiatList ->
                Fiats.updateFiats(fiatList)
            }
        }
    }
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.tip.wc.WCUnlockEvent

class RefreshDappJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshDappJob"
    }

    override fun onRun(): Unit = runBlocking {
        val response = tipService.dapps()
        if (response.isSuccess && response.data != null) {
            MixinApplication.get().chainDapp.clear()
            MixinApplication.get().chainDapp.addAll(response.data!!)
            RxBus.publish(WCUnlockEvent())
        } else {
            jobManager.addJobInBackground(RefreshDappJob())
        }
    }
}

package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class RefreshAddressJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAddressJob"
    }

    override fun onRun() {
        val response = addressService.addresses(assetId).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            response.data?.let {
                addressDao().insertList(it)
            }
        }
    }
}

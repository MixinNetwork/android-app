package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class RefreshAddressJob(private val addressId: String) : BaseJob(Params(PRIORITY_UI_HIGH)
    .addTags(RefreshAddressJob.GROUP).requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAddressJob"
    }

    override fun onRun() {
        val response = addressService.address(addressId).execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            response.data?.let {
                addressDao.insert(it)
            }
        }
    }
}
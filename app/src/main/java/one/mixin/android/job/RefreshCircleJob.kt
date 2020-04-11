package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.insertUpdate

class RefreshCircleJob(val circleId: String) : BaseJob(Params(PRIORITY_UI_HIGH).groupBy("refresh_circles").requireNetwork().persist()) {

    override fun onRun() {
        val circleResponse = circleService.getCircle(circleId).execute().body()
        if (circleResponse?.isSuccess == true) {
            circleResponse.data?.let { item ->
                circleDao.insertUpdate(item)
            }
        }
    }
}

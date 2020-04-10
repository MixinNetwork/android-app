package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.db.insertUpdate
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean

class RefreshCirclesJob : BaseJob(Params(PRIORITY_UI_HIGH).groupBy("refresh_circles").requireNetwork().persist()) {

    override fun onRun() {
        val circleResponse = circleService.getCircles().execute().body()
        if (circleResponse?.isSuccess == true) {
            circleResponse.data?.forEach { item ->
                circleDao.insertUpdate(item)
            }
            MixinApplication.get().applicationContext.defaultSharedPreferences.putBoolean(Constants.CIRCLE.CIRCLE_REFRESH, true)
        }
    }
}

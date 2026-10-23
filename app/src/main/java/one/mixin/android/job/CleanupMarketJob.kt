package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class CleanupMarketJob : BaseJob(
    Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).persist(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "CleanupMarketJob"
    }

    override fun onRun() {
        runBlocking {
            val deletedCount = marketDao.deleteMarketsWithoutCategoryRankOrFavorite()
            Timber.d("CleanupMarketJob deleted $deletedCount markets")
        }
    }
}

package one.mixin.android.ui.home.web3.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import one.mixin.android.R
import java.util.concurrent.TimeUnit

class MarketSinglePriceBtcAppWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetWork(context)
        scheduleWidgetWorkNow(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleWidgetWork(context)
        scheduleWidgetWorkNow(context)
        appWidgetIds.forEach { appWidgetId ->
            val views: RemoteViews = buildRemoteViews(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun scheduleWidgetWork(context: Context) {
        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<MarketSinglePriceWidgetWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(MarketSinglePriceWidgetWorker.buildInputData(COIN_ID))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views: RemoteViews = MarketSinglePriceWidgetRenderer.buildRemoteViews(
            context = context,
            coinId = COIN_ID,
            title = "BTC ${context.getString(R.string.Price)}",
        )
        return views
    }

    private fun scheduleWidgetWorkNow(context: Context) {
        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<MarketSinglePriceWidgetWorker>()
            .setConstraints(constraints)
            .setInputData(MarketSinglePriceWidgetWorker.buildInputData(COIN_ID))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        private const val COIN_ID: String = "bitcoin"
        private const val UNIQUE_ONE_TIME_WORK_NAME: String = "MarketSinglePriceWidgetWorker.BTC.Once"
        private const val UNIQUE_PERIODIC_WORK_NAME: String = "MarketSinglePriceWidgetWorker.BTC"

        fun notifyWidgetDataChanged(context: Context) {
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MarketSinglePriceBtcAppWidgetProvider::class.java)
            val appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { appWidgetId ->
                val views: RemoteViews = MarketSinglePriceWidgetRenderer.buildRemoteViews(context, COIN_ID, "BTC Price")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

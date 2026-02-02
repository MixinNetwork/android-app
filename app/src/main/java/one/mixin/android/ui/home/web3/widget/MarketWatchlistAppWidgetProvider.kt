@file:Suppress("DEPRECATION")

package one.mixin.android.ui.home.web3.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import one.mixin.android.R
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import java.util.concurrent.TimeUnit

class MarketWatchlistAppWidgetProvider : AppWidgetProvider() {

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
            val views = buildRemoteViews(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.market_watchlist_list)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_market_watchlist)
        val serviceIntent = Intent(context, MarketWatchlistRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.market_watchlist_list, serviceIntent)
        views.setEmptyView(R.id.market_watchlist_list, R.id.market_watchlist_empty)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAV_ITEM_ID, R.id.nav_market)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            pendingIntentImmutableFlags(PendingIntent.FLAG_UPDATE_CURRENT),
        )
        views.setOnClickPendingIntent(R.id.market_watchlist_header, openAppPendingIntent)

        val templateIntent = Intent(context, WalletActivity::class.java).apply {
            putExtra(WalletActivity.DESTINATION, WalletActivity.Destination.Market)
        }
        val templatePendingIntent = PendingIntent.getActivity(
            context,
            0,
            templateIntent,
            pendingIntentFillInTemplateFlags(PendingIntent.FLAG_UPDATE_CURRENT),
        )
        views.setPendingIntentTemplate(R.id.market_watchlist_list, templatePendingIntent)

        return views
    }

    private fun pendingIntentImmutableFlags(flags: Int): Int {
        return flags or PendingIntent.FLAG_IMMUTABLE
    }

    private fun pendingIntentFillInTemplateFlags(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags or PendingIntent.FLAG_MUTABLE
        } else {
            flags
        }
    }

    private fun scheduleWidgetWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWorkRequest = PeriodicWorkRequestBuilder<MarketWatchlistWidgetWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest,
        )
    }

    private fun scheduleWidgetWorkNow(context: Context) {
        val constraints: Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<MarketWatchlistWidgetWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest,
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME: String = "MarketWatchlistWidgetWorker"
        private const val UNIQUE_ONE_TIME_WORK_NAME: String = "MarketWatchlistWidgetWorker.Once"

        fun notifyWidgetDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MarketWatchlistAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.market_watchlist_list)
            }
        }
    }
}

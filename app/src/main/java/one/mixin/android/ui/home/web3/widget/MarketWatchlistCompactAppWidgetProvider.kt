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
import java.util.concurrent.TimeUnit

class MarketWatchlistCompactAppWidgetProvider : AppWidgetProvider() {

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
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.market_watchlist_compact_list)
        }
    }

    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val views: RemoteViews = RemoteViews(context.packageName, R.layout.widget_market_watchlist_compact)

        val headerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAV_ITEM_ID, R.id.nav_market)
        }
        val headerPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            headerIntent,
            pendingIntentImmutableFlags(PendingIntent.FLAG_UPDATE_CURRENT),
        )
        views.setOnClickPendingIntent(R.id.market_watchlist_compact_header, headerPendingIntent)

        val serviceIntent = Intent(context, MarketWatchlistRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_IS_COMPACT, true)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.market_watchlist_compact_list, serviceIntent)
        views.setEmptyView(R.id.market_watchlist_compact_list, R.id.market_watchlist_compact_empty)

        val clickTemplate = Intent(context, one.mixin.android.ui.wallet.WalletActivity::class.java).apply {
            putExtra(one.mixin.android.ui.wallet.WalletActivity.DESTINATION, one.mixin.android.ui.wallet.WalletActivity.Destination.Market)
        }
        val pendingIntentTemplate = PendingIntent.getActivity(
            context,
            0,
            clickTemplate,
            pendingIntentFillInTemplateFlags(PendingIntent.FLAG_UPDATE_CURRENT),
        )
        views.setPendingIntentTemplate(R.id.market_watchlist_compact_list, pendingIntentTemplate)
        return views
    }

    private fun scheduleWidgetWork(context: Context) {
        val constraints: Constraints = Constraints.Builder()
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
        val request = OneTimeWorkRequestBuilder<MarketWatchlistWidgetWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
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

    companion object {
        const val EXTRA_IS_COMPACT: String = "extra_is_compact"
        private const val UNIQUE_WORK_NAME: String = "MarketWatchlistWidgetWorker"
        private const val UNIQUE_ONE_TIME_WORK_NAME: String = "MarketWatchlistWidgetWorker.Once"

        fun notifyWidgetDataChanged(context: Context) {
            val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MarketWatchlistCompactAppWidgetProvider::class.java)
            val appWidgetIds: IntArray = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.market_watchlist_compact_list)
            }
        }
    }
}

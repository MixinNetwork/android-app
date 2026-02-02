package one.mixin.android.ui.home.web3.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.MarketDetailsFragment
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.io.File
import java.math.BigDecimal

class MarketWatchlistRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent,
) : RemoteViewsService.RemoteViewsFactory {

    private var marketItems: List<MarketItem> = emptyList()
    private val isCompact: Boolean = intent.getBooleanExtra(MarketWatchlistCompactAppWidgetProvider.EXTRA_IS_COMPACT, false)

    override fun onCreate() {
        marketItems = emptyList()
    }

    override fun onDataSetChanged() {
        val sharedPreferences: android.content.SharedPreferences = context.defaultSharedPreferences
        val sortValue: Int = sharedPreferences.getInt(Constants.Account.PREF_MARKET_ORDER, 0)
        val sort: MarketSort = MarketSort.fromValue(sortValue)
        val limit: Int = 30
        val database: MixinDatabase = MixinDatabase.getDatabase(context)
        marketItems = runCatching {
            runBlocking {
                database.marketDao().getFavoredWeb3MarketsList(limit, sort.value)
            }
        }.getOrDefault(emptyList())
    }

    override fun onDestroy() {
        marketItems = emptyList()
    }

    override fun getCount(): Int {
        return marketItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val itemLayout: Int = if (isCompact) {
            R.layout.item_market_watchlist_widget_compact
        } else {
            R.layout.item_market_watchlist_widget
        }
        val views: RemoteViews = RemoteViews(context.packageName, itemLayout)
        val item: MarketItem = marketItems.getOrNull(position) ?: return views
        val symbol: String = Fiats.getSymbol()
        val rate: BigDecimal = runCatching { BigDecimal(Fiats.getRate()) }.getOrDefault(BigDecimal.ONE)
        val priceText: String = buildPriceText(item, symbol, rate)
        val percentageValue: BigDecimal = runCatching { BigDecimal(item.priceChangePercentage24H) }.getOrDefault(BigDecimal.ZERO)
        val isRising: Boolean = percentageValue >= BigDecimal.ZERO
        val percentageText: String = "${percentageValue.numberFormat2()}%"
        val iconBitmap: Bitmap? = loadCachedBitmap(item.coinId, CACHE_SUFFIX_ICON)
        if (isCompact) {
            if (iconBitmap != null) {
                views.setImageViewBitmap(R.id.market_watchlist_compact_icon, iconBitmap)
            } else {
                views.setImageViewResource(R.id.market_watchlist_compact_icon, R.drawable.ic_avatar_place_holder)
            }
            views.setTextViewText(R.id.market_watchlist_compact_price, priceText)
            views.setTextViewText(R.id.market_watchlist_compact_percentage, percentageText)
        } else {
            if (iconBitmap != null) {
                views.setImageViewBitmap(R.id.market_watchlist_icon, iconBitmap)
            } else {
                views.setImageViewResource(R.id.market_watchlist_icon, R.drawable.ic_avatar_place_holder)
            }
            val sparklineBitmap: Bitmap? = loadCachedBitmap(item.coinId, CACHE_SUFFIX_SPARKLINE)
            if (sparklineBitmap != null) {
                views.setImageViewBitmap(R.id.market_watchlist_sparkline, sparklineBitmap)
            } else {
                views.setImageViewResource(R.id.market_watchlist_sparkline, R.drawable.ic_market_sparkline_placeholder)
            }
            views.setTextViewText(R.id.market_watchlist_symbol, item.symbol)
            views.setTextViewText(R.id.market_watchlist_name, item.name)
            views.setTextViewText(R.id.market_watchlist_price, priceText)
            views.setTextViewText(R.id.market_watchlist_percentage, percentageText)
        }
        val percentageColor: Int = if (isRising) {
            context.getColor(R.color.colorGreen)
        } else {
            context.getColor(R.color.colorRed)
        }
        if (isCompact) {
            views.setTextColor(R.id.market_watchlist_compact_percentage, percentageColor)
        } else {
            views.setTextColor(R.id.market_watchlist_percentage, percentageColor)
        }
        val fillInIntent: Intent = Intent().apply {
            putExtra(MarketDetailsFragment.ARGS_MARKET, item)
        }
        val clickContainerId: Int = if (isCompact) {
            R.id.market_watchlist_compact_item_container
        } else {
            R.id.market_watchlist_item_container
        }
        views.setOnClickFillInIntent(clickContainerId, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        val item: MarketItem? = marketItems.getOrNull(position)
        return item?.coinId?.hashCode()?.toLong() ?: position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    private fun buildPriceText(item: MarketItem, symbol: String, rate: BigDecimal): String {
        val currentPrice: BigDecimal = runCatching { BigDecimal(item.currentPrice) }.getOrDefault(BigDecimal.ZERO)
        return "$symbol${currentPrice.multiply(rate).priceFormat()}"
    }

    private fun loadCachedBitmap(coinId: String, suffix: String): Bitmap? {
        val dir: File = File(context.cacheDir, CACHE_DIR_NAME)
        val file: File = File(dir, "${coinId}_$suffix.png")
        if (!file.exists()) {
            return null
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private companion object {
        private const val CACHE_DIR_NAME: String = "market_watchlist_widget"
        private const val CACHE_SUFFIX_ICON: String = "icon"
        private const val CACHE_SUFFIX_SPARKLINE: String = "sparkline"
    }
}

package one.mixin.android.ui.home.web3.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import kotlinx.coroutines.runBlocking
import one.mixin.android.R
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.MarketDetailsFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.io.File
import java.math.BigDecimal

object MarketSinglePriceWidgetRenderer {

    const val EXTRA_COIN_ID: String = "extra_coin_id"

    fun buildRemoteViews(context: Context, coinId: String, title: String): RemoteViews {
        val views: RemoteViews = RemoteViews(context.packageName, R.layout.widget_market_single_price)
        views.setTextViewText(R.id.market_single_title, title)

        val database: MixinDatabase = MixinDatabase.getDatabase(context)
        val marketItem: MarketItem? = runCatching {
            runBlocking {
                database.marketDao().findMarketItemByCoinId(coinId)
            }
        }.getOrNull()

        if (marketItem == null) {
            val openIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                context,
                coinId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.market_single_root, openPendingIntent)
            views.setTextViewText(R.id.market_single_symbol_name, "")
            views.setTextViewText(R.id.market_single_price, "")
            views.setTextViewText(R.id.market_single_percentage, "")
            views.setImageViewResource(R.id.market_single_icon, R.drawable.ic_avatar_place_holder)
            return views
        }

        val iconBitmap: Bitmap? = loadCachedBitmap(context, marketItem.coinId, CACHE_SUFFIX_ICON)
        if (iconBitmap != null) {
            views.setImageViewBitmap(R.id.market_single_icon, iconBitmap)
        } else {
            views.setImageViewResource(R.id.market_single_icon, R.drawable.ic_avatar_place_holder)
        }

        val nameText: String = marketItem.name
        views.setTextViewText(R.id.market_single_symbol_name, "$nameText")

        val fiatSymbol: String = Fiats.getSymbol()
        val rate: BigDecimal = runCatching { BigDecimal(Fiats.getRate()) }.getOrDefault(BigDecimal.ONE)
        val currentPrice: BigDecimal = runCatching { BigDecimal(marketItem.currentPrice) }.getOrDefault(BigDecimal.ZERO)
        val priceText: String = "$fiatSymbol${currentPrice.multiply(rate).priceFormat()}"
        views.setTextViewText(R.id.market_single_price, priceText)

        val percentageValue: BigDecimal = runCatching { BigDecimal(marketItem.priceChangePercentage24H) }.getOrDefault(BigDecimal.ZERO)
        val isRising: Boolean = percentageValue >= BigDecimal.ZERO
        val percentageText: String = "${percentageValue.numberFormat2()}%"
        val percentageDisplayText: String = if (isRising) "+$percentageText" else percentageText
        views.setTextViewText(R.id.market_single_percentage, percentageDisplayText)
        val percentageColor: Int = if (isRising) {
            context.getColor(R.color.colorGreen)
        } else {
            context.getColor(R.color.colorRed)
        }
        views.setTextColor(R.id.market_single_percentage, percentageColor)

        val openIntent = Intent(context, WalletActivity::class.java).apply {
            putExtra(WalletActivity.DESTINATION, WalletActivity.Destination.Market)
            putExtra(MarketDetailsFragment.ARGS_MARKET, marketItem)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            coinId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.market_single_root, openPendingIntent)
        return views
    }

    private fun loadCachedBitmap(context: Context, coinId: String, suffix: String): Bitmap? {
        val dir: File = File(context.cacheDir, CACHE_DIR_NAME)
        val file: File = File(dir, "${coinId}_$suffix.png")
        if (!file.exists()) {
            return null
        }
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private const val CACHE_DIR_NAME: String = "market_watchlist_widget"
    private const val CACHE_SUFFIX_ICON: String = "icon"
}

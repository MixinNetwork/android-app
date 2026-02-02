package one.mixin.android.ui.home.web3.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.caverock.androidsvg.SVG
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.MarketCoinDao
import one.mixin.android.db.MarketDao
import one.mixin.android.db.MarketFavoredDao
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.market.MarketFavored
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal

@HiltWorker
class MarketWatchlistWidgetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val routeService: RouteService,
    private val userService: UserService,
    private val marketDao: MarketDao,
    private val marketFavoredDao: MarketFavoredDao,
    private val marketCoinDao: MarketCoinDao,
) : one.mixin.android.worker.BaseWork(context, parameters) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    override suspend fun onRun(): Result {
        val limit: Int = 500
        val now = nowInUtc()
        val response = requestRouteAPI(
            invokeNetwork = { routeService.markets(category = CATEGORY_FAVORITE, limit = limit) },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = { resp ->
                val list = resp.data ?: return@requestRouteAPI Result.failure()
                val favoredList: List<MarketFavored> = list.map { market ->
                    MarketFavored(
                        coinId = market.coinId,
                        isFavored = true,
                        createdAt = now,
                    )
                }
                marketFavoredDao.insertList(favoredList)
                marketDao.upsertList(list)
                val newMarketCoins: List<MarketCoin> = list.flatMap { market ->
                    market.assetIds?.map { assetId ->
                        MarketCoin(
                            coinId = market.coinId,
                            assetId = assetId,
                            createdAt = now,
                        )
                    } ?: emptyList()
                }
                val remoteAssetsByCoinId: Map<String, List<String>> = list.associate { market ->
                    market.coinId to (market.assetIds ?: emptyList())
                }
                for ((coinId, remoteAssetIds) in remoteAssetsByCoinId) {
                    val localAssetIds: List<String> = marketCoinDao.findTokenIdsByCoinId(coinId)
                    val assetIdsToDelete: List<String> = localAssetIds.filter { it !in remoteAssetIds }
                    if (assetIdsToDelete.isNotEmpty()) {
                        marketCoinDao.deleteByCoinIdAndAssetIds(coinId, assetIdsToDelete)
                    }
                }
                marketCoinDao.insertIgnoreList(newMarketCoins)
                cacheWidgetImages(list)
                MarketWatchlistAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
                MarketWatchlistCompactAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
                Result.success()
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
            },
        )
        return response ?: Result.failure()
    }

    private suspend fun cacheWidgetImages(markets: List<Market>) {
        val dir: File = File(applicationContext.cacheDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val shouldUseColorReversed: Boolean = applicationContext.defaultSharedPreferences
            .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
        val items: List<Market> = markets.take(WIDGET_CACHE_LIMIT)
        for (market in items) {
            cacheIcon(dir, market.coinId, market.iconUrl)
            val percentageText: String = market.priceChangePercentage24H
            val percentageValue: BigDecimal = runCatching { BigDecimal(percentageText) }.getOrDefault(BigDecimal.ZERO)
            val isRising: Boolean = percentageValue >= BigDecimal.ZERO
            val sparklineUrl: String = market.sparklineIn24h
            cacheSparkline(dir, market.coinId, sparklineUrl, isRising, shouldUseColorReversed)
        }
        MarketWatchlistCompactAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
    }

    private suspend fun cacheIcon(dir: File, coinId: String, url: String) {
        withContext(Dispatchers.IO) {
            val request: Request = Request.Builder().url(url).build()
            val response = runCatching { okHttpClient.newCall(request).execute() }.getOrNull() ?: return@withContext
            if (!response.isSuccessful) {
                response.close()
                return@withContext
            }
            val bytes: ByteArray = response.body?.bytes() ?: run {
                response.close()
                return@withContext
            }
            response.close()
            val iconSizePx: Int = dpToPx(ICON_SIZE_DP)
            val decodedBitmap: Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val bitmap: Bitmap = if (decodedBitmap != null) {
                decodedBitmap
            } else {
                val svgText: String = runCatching { String(bytes) }.getOrNull() ?: return@withContext
                val svg: SVG = runCatching { SVG.getFromString(svgText) }.getOrNull() ?: return@withContext
                svg.setDocumentWidth(iconSizePx.toFloat())
                svg.setDocumentHeight(iconSizePx.toFloat())
                val picture = svg.renderToPicture(iconSizePx, iconSizePx)
                val drawable: PictureDrawable = PictureDrawable(picture)
                val iconBitmap: Bitmap = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(iconBitmap)
                drawable.setBounds(0, 0, iconSizePx, iconSizePx)
                drawable.draw(canvas)
                iconBitmap
            }
            val scaledBitmap: Bitmap = if (bitmap.width != iconSizePx || bitmap.height != iconSizePx) {
                Bitmap.createScaledBitmap(bitmap, iconSizePx, iconSizePx, true)
            } else {
                bitmap
            }
            val file: File = File(dir, "${coinId}_${CACHE_SUFFIX_ICON}.png")
            runCatching {
                FileOutputStream(file).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private suspend fun cacheSparkline(
        dir: File,
        coinId: String,
        url: String,
        isRising: Boolean,
        isColorReversed: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            val request: Request = Request.Builder().url(url).build()
            val response = runCatching { okHttpClient.newCall(request).execute() }.getOrNull() ?: return@withContext
            if (!response.isSuccessful) {
                response.close()
                return@withContext
            }
            val stream = response.body?.byteStream() ?: run {
                response.close()
                return@withContext
            }
            val svg: SVG = runCatching { SVG.getFromInputStream(stream) }.getOrNull() ?: run {
                response.close()
                return@withContext
            }
            response.close()

            val widthPx: Int = dpToPx(SPARKLINE_WIDTH_DP)
            val heightPx: Int = dpToPx(SPARKLINE_HEIGHT_DP)
            svg.setDocumentWidth(widthPx.toFloat())
            svg.setDocumentHeight(heightPx.toFloat())
            val picture = svg.renderToPicture(widthPx, heightPx)
            val drawable: PictureDrawable = PictureDrawable(picture)
            val colorRes: Int = when {
                isRising && !isColorReversed -> R.color.wallet_green
                isRising && isColorReversed -> R.color.wallet_pink
                !isRising && !isColorReversed -> R.color.wallet_pink
                else -> R.color.wallet_green
            }
            val colorInt: Int = ContextCompat.getColor(applicationContext, colorRes)
            drawable.setColorFilter(colorInt, android.graphics.PorterDuff.Mode.SRC_IN)

            val bitmap: Bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, widthPx, heightPx)
            drawable.draw(canvas)

            val file: File = File(dir, "${coinId}_${CACHE_SUFFIX_SPARKLINE}.png")
            runCatching {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density: Float = applicationContext.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private companion object {
        private const val CATEGORY_FAVORITE: String = "favorite"
        private const val CACHE_DIR_NAME: String = "market_watchlist_widget"
        private const val CACHE_SUFFIX_ICON: String = "icon"
        private const val CACHE_SUFFIX_SPARKLINE: String = "sparkline"
        private const val WIDGET_CACHE_LIMIT: Int = 30
        private const val ICON_SIZE_DP: Int = 36
        private const val SPARKLINE_WIDTH_DP: Int = 60
        private const val SPARKLINE_HEIGHT_DP: Int = 24
    }
}

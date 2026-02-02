package one.mixin.android.ui.home.web3.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.caverock.androidsvg.SVG
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.Constants
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.MarketDao
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.Market
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class MarketSinglePriceWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val routeService: RouteService,
    private val userService: UserService,
    private val marketDao: MarketDao,
) : one.mixin.android.worker.BaseWork(appContext, params) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    override suspend fun onRun(): Result {
        val coinId: String = inputData.getString(EXTRA_COIN_ID) ?: return Result.failure()
        val response = requestRouteAPI(
            invokeNetwork = { routeService.fetchMarket(listOf(coinId)) },
            successBlock = { resp ->
                val markets: List<Market> = resp.data ?: return@requestRouteAPI Result.failure()
                marketDao.upsertList(markets)
                val market: Market? = markets.firstOrNull { it.coinId == coinId } ?: markets.firstOrNull()
                if (market != null) {
                    cacheIcon(coinId, market.iconUrl)
                }
                notifyWidgets(coinId)
                Result.success()
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
            },
        )
        return response ?: Result.failure()
    }

    private fun notifyWidgets(coinId: String) {
        when (coinId) {
            COIN_ID_BITCOIN -> MarketSinglePriceBtcAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
            COIN_ID_ETHEREUM -> MarketSinglePriceEthAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
            COIN_ID_SOLANA -> MarketSinglePriceSolAppWidgetProvider.notifyWidgetDataChanged(applicationContext)
        }
    }

    private suspend fun cacheIcon(coinId: String, url: String) {
        withContext(Dispatchers.IO) {
            val dir: File = File(applicationContext.cacheDir, CACHE_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val request: Request = Request.Builder().url(url).build()
            val bytes: ByteArray = runCatching {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use null
                    }
                    response.body?.bytes()
                }
            }.getOrNull() ?: return@withContext

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
            val file: File = File(dir, "${coinId}_$CACHE_SUFFIX_ICON.png")
            runCatching {
                FileOutputStream(file).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density: Float = applicationContext.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val EXTRA_COIN_ID: String = "extra_coin_id"
        private const val CACHE_DIR_NAME: String = "market_watchlist_widget"
        private const val CACHE_SUFFIX_ICON: String = "icon"
        private const val ICON_SIZE_DP: Int = 36

        private const val COIN_ID_BITCOIN: String = "bitcoin"
        private const val COIN_ID_ETHEREUM: String = "ethereum"
        private const val COIN_ID_SOLANA: String = "solana"

        fun buildInputData(coinId: String): Data {
            return Data.Builder()
                .putString(EXTRA_COIN_ID, coinId)
                .build()
        }
    }
}

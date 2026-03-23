package one.mixin.android.ui.home.web3.trade.perps

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.core.view.updateLayoutParams
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Scheme.HTTPS_TRADE
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.ActivityPerpsPositionShareBinding
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.round
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.vo.Fiats
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class PerpsPositionShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_POSITION = "args_position"
        private const val ARGS_POSITION_HISTORY = "args_position_history"
        private const val SHARE_QR_URL = "https://mixin.one/mm"
        private val MIN_DISPLAY_PNL_PERCENT = BigDecimal("-100")

        fun show(context: Context, position: PerpsPositionItem) {
            refreshScreenshot(context, 0x33000000)
            context.startActivity(Intent(context, PerpsPositionShareActivity::class.java).apply {
                putExtra(ARGS_POSITION, position)
            })
        }

        fun show(context: Context, positionHistory: PerpsPositionHistoryItem) {
            refreshScreenshot(context, 0x33000000)
            context.startActivity(Intent(context, PerpsPositionShareActivity::class.java).apply {
                putExtra(ARGS_POSITION_HISTORY, positionHistory)
            })
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityPerpsPositionShareBinding

    private val position: PerpsPositionItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)
    }

    private val positionHistory: PerpsPositionHistoryItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_POSITION_HISTORY, PerpsPositionHistoryItem::class.java)
    }

    private val quoteColorReversed: Boolean by lazy {
        defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        binding = ActivityPerpsPositionShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getScreenshot()?.let {
            supportsS({
                binding.overlay.background = it.toDrawable(resources)
                binding.overlay.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR))
            }, {
                binding.container.background = it.blurBitmap(25).toDrawable(resources)
            })
        }

        binding.content.updateLayoutParams<MarginLayoutParams> {
            topMargin = 80.dp
        }
        binding.iconFl.round(6.dp)
        binding.qr.post { binding.qr.round(4.dp) }

        val hasContent = bindContent()
        if (!hasContent) {
            finish()
            return
        }
        bindFooter()

        binding.apply {
            share.setOnClickListener {
                onShare()
            }
            copy.setOnClickListener {
                onCopy()
            }
            save.setOnClickListener {
                onSave()
            }
            close.setOnClickListener {
                onBackPressed()
            }
            container.setOnClickListener {
                onBackPressed()
            }
        }

        applyFadeInAnimation(binding.root)
    }

    private fun bindContent(): Boolean {
        val open = position
        if (open != null) {
            val pnlAmount = open.unrealizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
            val pnlPercent = calculatePnlPercent(
                margin = open.margin.toBigDecimalSafely(),
                pnlAmount = pnlAmount,
            )

            bindCard(
                iconUrl = open.iconUrl,
                side = open.side,
                leverage = open.leverage,
                pnlPercent = pnlPercent,
                isProfit = pnlAmount >= BigDecimal.ZERO,
                tokenSymbol = open.tokenSymbol?:"",
                entryPrice = open.entryPrice,
                latestLabel = getString(R.string.Latest_Price),
                latestPrice = open.markPrice ?: open.entryPrice,
            )
            return true
        }

        val closed = positionHistory ?: return false
        val pnlAmount = closed.realizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
        val pnlPercent = calculateClosedPercent(
            entryPrice = closed.entryPrice,
            closePrice = closed.closePrice,
            side = closed.side,
            leverage = closed.leverage,
        )

        bindCard(
            iconUrl = closed.iconUrl,
            side = closed.side,
            leverage = closed.leverage,
            pnlPercent = pnlPercent,
            isProfit = pnlAmount >= BigDecimal.ZERO,
            tokenSymbol = closed.tokenSymbol?:"",
            entryPrice = closed.entryPrice,
            latestLabel = getString(R.string.Close_Price),
            latestPrice = closed.closePrice,
        )
        return true
    }

    private fun bindCard(
        iconUrl: String?,
        side: String,
        leverage: Int,
        pnlPercent: BigDecimal,
        isProfit: Boolean,
        tokenSymbol: String,
        entryPrice: String,
        latestLabel: String,
        latestPrice: String,
    ) {
        binding.assetIcon.loadImage(iconUrl, R.drawable.ic_avatar_place_holder)
        binding.pnlTv.text = formatSignedPercent(pnlPercent)

        val isLong = side.equals("long", ignoreCase = true)
        binding.sideTagTv.text = "${if (isLong) getString(R.string.Long) else getString(R.string.Short)} $tokenSymbol".trim()
        binding.leverageTagTv.text = getString(R.string.Perpetual_Leverage_Format, leverage)

        val useProfitStyle = if (quoteColorReversed) !isProfit else isProfit
        binding.topCard.setBackgroundResource(
            if (useProfitStyle) R.drawable.bg_perps_share_card_profit else R.drawable.bg_perps_share_card_loss
        )
        binding.trendImage.setImageResource(
            if (isProfit) R.drawable.ic_perps_profit else R.drawable.ic_perps_loss
        )

        binding.entryValueTv.text = formatFiat(entryPrice)
        binding.latestLabelTv.text = latestLabel
        binding.latestValueTv.text = formatFiat(latestPrice)
    }

    private fun bindFooter() {
        Session.getAccount()?.identityNumber.let {
            val qrcodeContent = it?.let { referral -> "${SHARE_QR_URL}?ref=$referral" } ?: SHARE_QR_URL
            val qrCode = qrcodeContent.generateQRCode(72.dp, 8.dp).first
            binding.qr.setImageBitmap(qrCode)
        }
    }

    private fun applyFadeInAnimation(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(null)
    }

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = binding.topCard.drawToBitmap()
            val file = File(cacheDir, "${buildFileName()}_position.png")
            saveBitmapToFile(file, bitmap)
            val uri = FileProvider.getUriForFile(this@PerpsPositionShareActivity, BuildConfig.APPLICATION_ID + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/png"
            share.putExtra(Intent.EXTRA_STREAM, uri)
            finish()
            startActivity(Intent.createChooser(share, getString(R.string.Share)))
        }
    }

    private val onCopy: () -> Unit = {
        val marketId = position?.marketId ?: positionHistory?.marketId
        val marketLink = marketId?.let { "$HTTPS_TRADE?type=perps&market=$it" } ?: HTTPS_TRADE
        val link = Session.getAccount()?.identityNumber?.let { "$marketLink&referral=$it" } ?: marketLink
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        finish()
        toast(R.string.copied_to_clipboard)
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            delay(100)
            val bitmap = binding.topCard.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "${buildFileName()}_position.png")
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(this@PerpsPositionShareActivity, arrayOf(file.toString()), null, null)
            finish()
            toast(getString(R.string.Save_to, dir.path))
        }
    }

    private fun buildFileName(): String {
        val name = position?.displaySymbol
            ?: position?.tokenSymbol
            ?: positionHistory?.displaySymbol
            ?: positionHistory?.tokenSymbol
            ?: "perps"
        return name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
    }

    private fun calculatePnlPercent(
        margin: BigDecimal?,
        pnlAmount: BigDecimal,
    ): BigDecimal {
        if (margin == null || margin <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        return pnlAmount
            .divide(margin, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
    }

    private fun calculateClosedPercent(
        entryPrice: String?,
        closePrice: String?,
        side: String,
        leverage: Int,
    ): BigDecimal {
        val entry = entryPrice.toBigDecimalSafely() ?: return BigDecimal.ZERO
        val close = closePrice.toBigDecimalSafely() ?: return BigDecimal.ZERO
        if (entry <= BigDecimal.ZERO || leverage <= 0) {
            return BigDecimal.ZERO
        }

        val direction = if (side.equals("short", ignoreCase = true)) BigDecimal(-1) else BigDecimal.ONE
        return close
            .subtract(entry)
            .divide(entry, 8, RoundingMode.HALF_UP)
            .multiply(BigDecimal(leverage))
            .multiply(BigDecimal(100))
            .multiply(direction)
    }

    private fun formatSignedPercent(value: BigDecimal): String {
        val displayValue = value.max(MIN_DISPLAY_PNL_PERCENT)
        val sign = when {
            displayValue > BigDecimal.ZERO -> "+"
            displayValue < BigDecimal.ZERO -> "-"
            else -> ""
        }
        val number = displayValue.abs().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "$sign$number%"
    }

    private fun formatFiat(value: String?): String {
        val price = value.toBigDecimalSafely() ?: BigDecimal.ZERO
        val fiatPrice = price.multiply(BigDecimal(Fiats.getRate()))
        return "${Fiats.getSymbol()}${fiatPrice.priceFormat()}"
    }

    private fun String?.toBigDecimalSafely(): BigDecimal? {
        return this?.toBigDecimalOrNull()
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

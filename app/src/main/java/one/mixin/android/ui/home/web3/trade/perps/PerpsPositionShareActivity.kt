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
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.referral.buildReferralCopyUrl
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.buildReferralShareUrl
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
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.applyReferralTitleTypeface
import one.mixin.android.ui.common.buildReferralDescription
import one.mixin.android.ui.common.isZeroPercent
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.vo.Fiats
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlin.math.min
import one.mixin.android.repository.ReferralRepository

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
    private val loadingDialog by lazy { LoadingProgressDialogFragment() }
    @Inject
    lateinit var referralRepository: ReferralRepository

    private val position: PerpsPositionItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)
    }

    private val positionHistory: PerpsPositionHistoryItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_POSITION_HISTORY, PerpsPositionHistoryItem::class.java)
    }

    private val quoteColorReversed: Boolean by lazy {
        defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }

    private var referralShareInfo: ReferralShareInfo? = null

    private val referralCode: String?
        get() = referralShareInfo?.code?.takeIf { it.isNotBlank() }
    private var isLoading = false

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

        binding.content.updateLayoutParams<FrameLayout.LayoutParams> {
            width = min(380.dp, (resources.displayMetrics.widthPixels - 80.dp).coerceAtLeast(0))
            topMargin = 80.dp
        }
        val hasContent = bindContent()
        if (!hasContent) {
            finish()
            return
        }

        binding.apply {
            share.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onShare()
            }
            copy.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onCopy()
            }
            save.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onSave()
            }
            close.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onBackPressed()
            }
            container.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onBackPressed()
            }
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                referralShareInfo = withContext(Dispatchers.IO) {
                    referralRepository.fetchDefaultReferralShareInfoOrNull(logLabel = "perps position share")
                }
                bindFooter()
                applyFadeInAnimation(binding.root)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.content.isVisible = !show
        if (show) {
            if (!loadingDialog.isAdded) {
                runCatching {
                    loadingDialog.show(supportFragmentManager, LoadingProgressDialogFragment.TAG)
                }
            }
        } else if (loadingDialog.isAdded) {
            loadingDialog.dismissAllowingStateLoss()
        }
    }

    private fun bindContent(): Boolean {
        val open = position
        if (open != null) {
            val pnlAmount = open.unrealizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
            val pnlPercent = (open.roe.toBigDecimalSafely() ?: BigDecimal.ZERO).multiply(BigDecimal(100))

            bindCard(
                iconUrl = open.iconUrl,
                side = open.side,
                leverage = open.leverage,
                pnlPercent = pnlPercent,
                isProfit = pnlAmount >= BigDecimal.ZERO,
                tokenSymbol = open.tokenSymbol?:"",
                entryPrice = open.entryPrice,
                latestLabel = getString(R.string.perps_current_price),
                latestPrice = open.markPrice ?: open.entryPrice,
            )
            return true
        }

        val closed = positionHistory ?: return false
        val pnlAmount = closed.realizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
        val pnlPercent = calculateClosedRoe(
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
        val info = referralShareInfo
        if (info != null) {
            binding.title.text = info.code
            binding.title.applyReferralTitleTypeface()
            val rebatePercent = info.rebatePercent
            if (rebatePercent.isNullOrBlank()) {
                binding.shareDescTv.isVisible = false
                binding.shareDescTv.minLines = 1
            } else {
                binding.shareDescTv.isVisible = true
                binding.shareDescTv.minLines = if (rebatePercent.isZeroPercent()) 2 else 1
                binding.shareDescTv.text = buildReferralDescription(this, rebatePercent)
            }
        } else {
            binding.shareDescTv.isVisible = true
            binding.shareDescTv.minLines = 1
            binding.title.text = getString(R.string.mixin_messenger)
            binding.shareDescTv.text = getString(R.string.share_desc)
        }
        val qrPadding = 8.dp
        val qrCode = currentQrUrl().generateQRCode(72.dp, qrPadding).first.roundQrBackground(qrPadding, 6.dp.toFloat())
        binding.qr.setImageBitmap(qrCode)
    }

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

    private fun applyFadeInAnimation(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(null)
    }

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = createShareBitmap()
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
        val link = buildReferralCopyUrl(
            referralCode = referralCode,
            defaultUrl = SHARE_QR_URL,
            legacyReferralUrl = Session.getAccount()?.identityNumber?.let { "$SHARE_QR_URL&referral=$it" },
        )
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        finish()
        toast(R.string.copied_to_clipboard)
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            delay(100)
            val bitmap = createShareBitmap()
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

    private fun createShareBitmap(): Bitmap {
        val closeVisibility = binding.close.visibility
        binding.close.visibility = View.INVISIBLE
        return try {
            binding.shareCardContainer.drawToBitmap()
        } finally {
            binding.close.visibility = closeVisibility
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

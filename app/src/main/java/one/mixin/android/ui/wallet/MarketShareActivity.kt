package one.mixin.android.ui.wallet

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Scheme.HTTPS_MARKET
import one.mixin.android.R
import one.mixin.android.api.referral.buildReferralCopyUrl
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.buildReferralShareUrl
import one.mixin.android.databinding.ActivityMarketShareBinding
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.round
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.applyReferralTitleTypeface
import one.mixin.android.ui.common.buildReferralDescription
import one.mixin.android.ui.common.isZeroPercent
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.repository.ReferralRepository
import one.mixin.android.util.analytics.AnalyticsTracker

@AndroidEntryPoint
class MarketShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_NAME = "name"
        private const val ARGS_COIN = "coin"
        private const val SHARE_QR_URL = "https://mixin.one/mm"

        private var cover: Bitmap? = null

        fun show(
            context: Context,
            cover: Bitmap,
            name: String,
            coinId: String,
        ) {
            refreshScreenshot(context, 0x33000000)
            this.cover = cover
            context.startActivity(Intent(context, MarketShareActivity::class.java).apply {
                putExtra(ARGS_NAME, name)
                putExtra(ARGS_COIN, coinId)
            })
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityMarketShareBinding
    @Inject
    lateinit var referralRepository: ReferralRepository
    private val loadingDialog by lazy { LoadingProgressDialogFragment() }
    private val name by lazy {
        intent.getStringExtra(ARGS_NAME)
    }
    private val coinId by lazy {
        intent.getStringExtra(ARGS_COIN)
    }
    private var referralShareInfo: ReferralShareInfo? = null
    private val referralCode: String?
        get() = referralShareInfo?.code?.takeIf { it.isNotBlank() }
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        binding = ActivityMarketShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getScreenshot()?.let {
            supportsS({
                binding.background.background = it.toDrawable(resources)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR))
            }, {
                binding.container.background = it.blurBitmap(25).toDrawable(resources)
            })
        }
        binding.llMarketShare.round(8.dp)
        binding.content.updateLayoutParams<MarginLayoutParams> {
            topMargin = 20.dp
        }
        binding.apply {
            share.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.SHARE)
                onShare()
            }
            copy.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.COPY)
                onCopy()
            }
            save.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.SAVE)
                onSave()
            }
            container.setOnClickListener {
                if (isLoading) return@setOnClickListener
                onBackPressed()
            }
        }
        syncActionContainerVisibility()
        lifecycleScope.launch {
            showLoading(true)
            try {
                referralShareInfo = withContext(Dispatchers.IO) {
                    referralRepository.fetchDefaultReferralShareInfoOrNull(logLabel = "market share")
                }
                val preparedCover = withContext(Dispatchers.Default) {
                    cover?.let {
                        trimTransparentPadding(it)
                            .cropBottom(8.dp)
                            .roundBottomCorners(8.dp.toFloat())
                    }
                }
                val qrCode = withContext(Dispatchers.Default) {
                    val qrPadding = 8.dp
                    currentQrUrl().generateQRCode(72.dp, qrPadding).first.roundQrBackground(qrPadding, 6.dp.toFloat())
                }
                preparedCover?.let(binding.image::setImageBitmap)
                bindFooter(qrCode)
                applyFadeInAnimation(binding.content)
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

    private fun bindFooter(qrCode: Bitmap) {
        val info = referralShareInfo
        if (info != null) {
            binding.title.text = info.code
            binding.title.applyReferralTitleTypeface()
            val rebatePercent = info.rebatePercent
            if (rebatePercent.isNullOrBlank()) {
                binding.shareDesc.isVisible = false
                binding.shareDesc.minLines = 1
            } else {
                binding.shareDesc.isVisible = true
                binding.shareDesc.minLines = if (rebatePercent.isZeroPercent()) 2 else 1
                binding.shareDesc.text = buildReferralDescription(this, rebatePercent)
            }
        } else {
            binding.shareDesc.isVisible = true
            binding.shareDesc.minLines = 1
            binding.title.text = getString(R.string.mixin_messenger)
            binding.shareDesc.text = getString(R.string.share_desc)
        }
        binding.qr.setImageBitmap(qrCode)
    }

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

    private fun syncActionContainerVisibility() {
        val hasVisibleAction = binding.share.isVisible || binding.copy.isVisible || binding.save.isVisible
        binding.bottom.isVisible = hasVisibleAction
        binding.bottom.updateLayoutParams<MarginLayoutParams> {
            topMargin = if (hasVisibleAction) 10.dp else 0
        }
    }

    private fun applyFadeInAnimation(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(null)
    }

    private fun trimTransparentPadding(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                if ((pixels[rowOffset + x] ushr 24) != 0) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }

        if (right < left || bottom < top) return bitmap
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
    }

    private fun Bitmap.cropBottom(cropHeight: Int): Bitmap {
        if (cropHeight <= 0 || height <= cropHeight) return this
        return Bitmap.createBitmap(this, 0, 0, width, height - cropHeight)
    }

    private fun Bitmap.roundBottomCorners(radius: Float): Bitmap {
        if (width <= 0 || height <= 0 || radius <= 0f) return this

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(this@roundBottomCorners, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                floatArrayOf(
                    0f, 0f,
                    0f, 0f,
                    radius, radius,
                    radius, radius,
                ),
                Path.Direction.CW,
            )
        }
        canvas.drawPath(path, paint)
        return output
    }

    private fun createShareBitmap(): Bitmap {
        val closeVisibility = binding.close.visibility
        binding.close.visibility = View.INVISIBLE
        return try {
            binding.llMarketShare.drawToBitmap()
        } finally {
            binding.close.visibility = closeVisibility
        }
    }

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = createShareBitmap()
            showLoading(true)
            try {
                val file = File(cacheDir, "$name.png")
                saveBitmapToFile(file, bitmap)
                val uri = FileProvider.getUriForFile(this@MarketShareActivity, BuildConfig.APPLICATION_ID + ".provider", file)
                val share = Intent()
                share.action = Intent.ACTION_SEND
                share.type = "image/png"
                share.putExtra(Intent.EXTRA_STREAM, uri)
                finish()
                startActivity(Intent.createChooser(share, getString(R.string.Share)))
            } finally {
                showLoading(false)
            }
        }
    }

    private val onCopy: () -> Unit = {
        val marketLink = "$HTTPS_MARKET/$coinId"
        val link = buildReferralCopyUrl(
            referralCode = referralCode,
            defaultUrl = marketLink,
            legacyReferralUrl = Session.getAccount()?.identityNumber?.let { "$marketLink?ref=$it" },
        )
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        finish()
        toast(R.string.copied_to_clipboard)
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            delay(100)
            val bitmap = createShareBitmap()
            showLoading(true)
            try {
                val dir = getPublicDownloadPath()
                dir.mkdirs()
                val file = File(dir, "$name.png")
                saveBitmapToFile(file, bitmap)
                MediaScannerConnection.scanFile(this@MarketShareActivity, arrayOf(file.toString()), null, null)
                finish()
                toast(getString(R.string.Save_to, dir.path))
            } finally {
                showLoading(false)
            }
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

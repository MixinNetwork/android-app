package one.mixin.android.ui.wallet

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaScannerConnection
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Scheme.HTTPS_MARKET
import one.mixin.android.R
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
import one.mixin.android.ui.wallet.fiatmoney.buildReferralShareUrl
import one.mixin.android.ui.wallet.fiatmoney.ReferralShareInfo
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale

@AndroidEntryPoint
class MarketShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_NAME = "name"
        private const val ARGS_COIN = "coin"
        private const val ARGS_REFERRAL_SHARE_INFO = "referral_share_info"
        private const val SHARE_QR_URL = "https://mixin.one/mm"
        private const val REFERRAL_REBATE_COLOR = "#FFEE70"
        private var cover: Bitmap? = null
        fun show(
            context: Context,
            cover: Bitmap,
            name: String,
            coinId: String,
            referralShareInfo: ReferralShareInfo? = null,
        ) {
            refreshScreenshot(context, 0x33000000)
            this.cover = cover
            context.startActivity(Intent(context, MarketShareActivity::class.java).apply {
                putExtra(ARGS_NAME, name)
                putExtra(ARGS_COIN, coinId)
                putExtra(ARGS_REFERRAL_SHARE_INFO, referralShareInfo)
            })
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityMarketShareBinding
    private val name by lazy {
        intent.getStringExtra(ARGS_NAME)
    }
    private val coinId by lazy {
        intent.getStringExtra(ARGS_COIN)
    }
    private val referralShareInfo by lazy {
        intent.getSerializableExtra(ARGS_REFERRAL_SHARE_INFO) as? ReferralShareInfo
    }
    private val referralCode by lazy {
        referralShareInfo?.code?.takeIf { it.isNotBlank() }
    }

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
        if (cover != null) {
            binding.image.setImageBitmap(cropAndScaleBitmap(cover!!, 24.dp, (80 - 24 + 32).dp))
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
            container.setOnClickListener {
                onBackPressed()
            }
        }

        applyFadeInAnimation(binding.root)
    }

    private fun bindFooter() {
        if (referralShareInfo != null) {
            binding.title.text = referralShareInfo?.code
            binding.shareDesc.text = buildReferralDescription(referralShareInfo!!.rebatePercent)
        } else {
            binding.title.text = getString(R.string.mixin_messenger)
            binding.shareDesc.text = getString(R.string.share_desc)
        }
        val qrCode = currentQrUrl().generateQRCode(72.dp, 8.dp).first
        binding.qr.setImageBitmap(qrCode)
    }

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

    private fun buildReferralDescription(rebatePercent: String): SpannableString {
        val text = getString(R.string.referral_share_desc, rebatePercent)
        val start = text.indexOf(rebatePercent)
        return SpannableString(text).apply {
            if (start >= 0) {
                setSpan(
                    ForegroundColorSpan(Color.parseColor(REFERRAL_REBATE_COLOR)),
                    start,
                    start + rebatePercent.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    start + rebatePercent.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    private fun applyFadeInAnimation(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(null)
    }

    private fun cropAndScaleBitmap(original: Bitmap, cropHeight: Int, y: Int): Bitmap {
        val croppedBitmap = Bitmap.createBitmap(
            original,
            0,
            0,
            original.width,
            original.height - cropHeight
        )
        val targetWidth = croppedBitmap.width - y
        val scale = targetWidth.toFloat() / croppedBitmap.width.toFloat()
        val targetHeight = (croppedBitmap.height * scale).toInt()
        val scaledBitmap = croppedBitmap.scale(targetWidth, targetHeight)

        return scaledBitmap
    }

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = binding.llMarketShare.drawToBitmap()
            val file = File(cacheDir, "$name.png")
            saveBitmapToFile(file, bitmap)
            val uri = FileProvider.getUriForFile(this@MarketShareActivity, BuildConfig.APPLICATION_ID + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/png"
            share.putExtra(Intent.EXTRA_STREAM, uri)
            finish()
            startActivity(Intent.createChooser(share, getString(R.string.Share)))
        }
    }

    private val onCopy: () -> Unit = {
        val link = referralCode?.let(::buildReferralShareUrl) ?: run {
            val marketLink = "$HTTPS_MARKET/$coinId"
            Session.getAccount()?.identityNumber?.let { "$marketLink?ref=$it" } ?: marketLink
        }
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        finish()
        toast(R.string.copied_to_clipboard)
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            delay(100)
            val bitmap = binding.llMarketShare.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "$name.png")
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(this@MarketShareActivity, arrayOf(file.toString()), null, null)
            finish()
            toast(getString(R.string.Save_to, dir.path))
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}

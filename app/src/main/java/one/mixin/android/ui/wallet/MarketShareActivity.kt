package one.mixin.android.ui.wallet

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Bundle
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
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MarketShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_NAME = "name"
        private var cover: Bitmap? = null
        fun show(context: Context, cover: Bitmap, name:String) {
            refreshScreenshot(context, 0x33000000)
            this.cover = cover
            context.startActivity(Intent(context, MarketShareActivity::class.java).apply {
                putExtra(ARGS_NAME, name)
            })
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityMarketShareBinding
    private val name by lazy {
        intent.getStringExtra(ARGS_NAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarketShareBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getScreenshot()?.let {
            supportsS({
                binding.background.background = BitmapDrawable(resources, it)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR))
            }, {
                binding.container.background = BitmapDrawable(resources, it.blurBitmap(25))
            })
        }
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding.test.round(8.dp)
        binding.content.updateLayoutParams<MarginLayoutParams> {
            topMargin = 20.dp
        }
        if (cover != null) {
            binding.image.setImageBitmap(cropAndScaleBitmap(cover!!, 8.dp, (80 - 24 + 32).dp))
        }
        Session.getAccount()?.system?.messenger?.releaseUrl?.let {
            val qrCode = it.generateQRCode(72.dp, 8.dp).first
            binding.qr.setImageBitmap(qrCode)
        }
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
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)

        return scaledBitmap
    }

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = binding.test.drawToBitmap()
            val file = File(cacheDir, "${name}.png")
            saveBitmapToFile(file, bitmap)
            val uri = FileProvider.getUriForFile(this@MarketShareActivity, BuildConfig.APPLICATION_ID + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/png"
            share.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(share, getString(R.string.Share)))
        }
    }

    private val onCopy: () -> Unit = {
        getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, Session.getAccount()?.system?.messenger?.releaseUrl))
        toast(R.string.copied_to_clipboard)
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            delay(100)
            val bitmap = binding.test.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "${name}.png")
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(this@MarketShareActivity, arrayOf(file.toString()), null, null)
            toast(getString(R.string.Save_to, dir.path))
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
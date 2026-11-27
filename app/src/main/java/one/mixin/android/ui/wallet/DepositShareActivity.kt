@file:Suppress("DEPRECATION")

package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ActivityDepositShareBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.deleteOnExists
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.toValidFileName
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import one.mixin.android.widget.BadgeCircleImageView
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class DepositShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_TOKEN = "token"
        private const val ARGS_WEB3_TOKEN = "web3_token"
        private const val ARGS_ADDRESS = "address"
        private const val ARGS_AMOUNT = "amount"
        private const val ARGS_AMOUNT_URL = "amount_url"
        private const val ARGS_USER = "user"
        private const val SHARE_QR_URL = "https://mixin.one/mm"

        fun show(context: Context, token: TokenItem?, address: String? = null, amountUrl: String? = null, amount: String? = null, user: User? = null) {
            refreshScreenshot(context, 0x33000000)
            context.startActivity(Intent(context, DepositShareActivity::class.java).apply {
                putExtra(ARGS_TOKEN, token)
                putExtra(ARGS_ADDRESS, address)
                putExtra(ARGS_AMOUNT, amount)
                putExtra(ARGS_AMOUNT_URL, amountUrl)
                putExtra(ARGS_USER, user)
            })
        }

        fun show(context: Context, web3Token: Web3TokenItem?, address: String? = null, amountUrl: String? = null, amount: String? = null) {
            refreshScreenshot(context, 0x33000000)
            context.startActivity(Intent(context, DepositShareActivity::class.java).apply {
                putExtra(ARGS_WEB3_TOKEN, web3Token)
                putExtra(ARGS_ADDRESS, address)
                putExtra(ARGS_AMOUNT, amount)
                putExtra(ARGS_AMOUNT_URL, amountUrl)
            })
        }
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityDepositShareBinding
    private val token: TokenItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
    }
    private val web3Token: Web3TokenItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_WEB3_TOKEN, Web3TokenItem::class.java)
    }
    private val user: User? by lazy {
        intent.extras?.getParcelableCompat(ARGS_USER, User::class.java)
    }
    private val address by lazy {
        intent.getStringExtra(ARGS_ADDRESS)
    }

    private val amountUrl by lazy {
        intent.getStringExtra(ARGS_AMOUNT_URL)
    }

    private val amount by lazy {
        intent.getStringExtra(ARGS_AMOUNT)
    }

    private val tokenSymbol: String?
        get() = token?.symbol ?: web3Token?.symbol

    private val tokenIconUrl: String?
        get() = token?.iconUrl ?: web3Token?.iconUrl

    private val tokenChainUrl: String?
        get() = token?.chainIconUrl ?: web3Token?.chainIcon

    private val tokenAssetId: String?
        get() = token?.assetId ?: web3Token?.assetId

    private val tokenChainName: String?
        get() = token?.chainName ?: web3Token?.chainName

    private val tokenDust: String
        get() = token?.dust ?: "0" // Web3TokenItem doesn't have dust, use default

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSystemUi = true
        super.onCreate(savedInstanceState)
        binding = ActivityDepositShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getScreenshot()?.let {
            supportsS({
                binding.background.setImageBitmap(it)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.REPEAT))
            }, {
                binding.background.setImageBitmap(it.blurBitmap(25))
            })
        }
        binding.iconFl.round(6.dp)
        binding.qr.post { binding.qr.round(4.dp) }
        binding.content.updateLayoutParams<MarginLayoutParams> {
            topMargin = 20.dp
        }

        setupUI()

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
            close.setOnClickListener {
                onBackPressed()
            }
        }
        Session.getAccount()?.identityNumber.let {
            val qrSize = 78.dp
            val qrPadding = 8.dp
            val qrCode = SHARE_QR_URL.generateQRCode(qrSize, qrPadding, outputSize = qrSize).first
                .roundQrBackground(qrPadding, 6.dp.toFloat())
            binding.qr.setImageBitmap(qrCode)
        }
        applyFadeInAnimation(binding.root)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val hasToken = token != null || web3Token != null
        if (user != null || !hasToken) {
            val u = user ?: Session.getAccount()?.toUser()
            binding.titleTv.text = u?.fullName
            binding.subTitleTv.text = getString(R.string.contact_mixin_id, u?.identityNumber ?: "")
            binding.containerLl.isVisible = false
            (amountUrl ?: address)?.let { addr ->
                val qrCode = addr.generateQRCode(200.dp, innerPadding = 20.dp, padding = 0).first
                binding.qrCode.setImageBitmap(qrCode)
                binding.icon.bg.loadImage(u?.avatarUrl, R.drawable.ic_avatar_place_holder)
                binding.icon.bg.borderWidth = 2.dp
                binding.icon.badge.borderWidth = 0
                binding.icon.badge.borderColor = Color.WHITE
                binding.icon.badge.setImageResource(R.drawable.ic_contacts_receive_blue)
                binding.icon.pos = BadgeCircleImageView.END_BOTTOM
            }
            binding.bottomTv.isVisible = true
            if (hasToken) {
                binding.bottomTv.text = getString(R.string.transfer_qrcode_prompt_amount, "$amount")
            } else {
                binding.bottomTv.setText(R.string.transfer_qrcode_prompt)
            }
        } else {
            binding.titleTv.text = getString(R.string.Deposit_to_Mixin, tokenSymbol ?: "")
            binding.subTitleTv.text = getString(R.string.Deposit_to_Mixin_sub, tokenSymbol ?: "")
            (amountUrl ?: address)?.let { addr ->
                val qrCode = addr.generateQRCode(200.dp, innerPadding = 32.dp, padding = 0).first
                binding.qrCode.setImageBitmap(qrCode)
                binding.icon.bg.loadImage(tokenIconUrl)
                binding.icon.badge.loadImage(tokenChainUrl, R.drawable.ic_avatar_place_holder)
            }

            val addr = if (token?.chainId == Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) amountUrl ?: "" else address ?: ""
            if (addr.length > 14) {
                val spannable = android.text.SpannableStringBuilder(addr)
                val black = colorFromAttribute(R.attr.text_primary)
                val gray = colorFromAttribute(R.attr.text_assist)
                val boldStyle = StyleSpan(Typeface.BOLD)

                spannable.setSpan(ForegroundColorSpan(gray), 8, addr.length - 6, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannable.setSpan(ForegroundColorSpan(black), 0, 8, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(boldStyle, 0, 8, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannable.setSpan(ForegroundColorSpan(black), addr.length - 6, addr.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), addr.length - 6, addr.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                binding.addressText.text = spannable
            } else {
                binding.addressText.text = addr
            }
            binding.addressTitle.setText(if (token?.chainId == Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID) R.string.Invoice else R.string.Address)
            binding.networkText.text = tokenChainName

            if (amount != null) {
                binding.minimumDepositTitle.setText(R.string.Amount)
                binding.minimumDepositText.text = "$amount"
            } else if (token != null) {
                binding.minimumDepositText.text = "$tokenDust $tokenSymbol"
            } else {
                binding.minimumDepositTitle.isInvisible = true
                binding.minimumDepositText.isInvisible = true
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

    private val onShare: () -> Unit = {
        lifecycleScope.launch {
            val bitmap = binding.test.drawToBitmap()
            val fileName = "${tokenSymbol}_deposit.png"
            val file = File(cacheDir, fileName)
            saveBitmapToFile(file, bitmap)
            val uri = FileProvider.getUriForFile(this@DepositShareActivity, BuildConfig.APPLICATION_ID + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/png"
            share.putExtra(Intent.EXTRA_STREAM, uri)
            finish()
            startActivity(Intent.createChooser(share, getString(R.string.Share)))
        }
    }

    private val onCopy: () -> Unit = {
        address?.let { addr ->
            getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, addr))
            finish()
            toast(R.string.copied_to_clipboard)
        }
    }

    private val onSave: () -> Unit = {
        lifecycleScope.launch {
            try {
                delay(100)
                if (isFinishing || isDestroyed) return@launch
                val bitmap = binding.test.drawToBitmap()
                val fileName = "${(tokenSymbol?.takeIf { it.isNotBlank() } ?: "mixin").toValidFileName()}_deposit.png"
                val savedPath =
                    withContext(Dispatchers.IO) {
                        saveDepositBitmap(bitmap, fileName)
                    }
                if (!isFinishing && !isDestroyed) {
                    finish()
                }
                toast(getString(R.string.Save_to, savedPath))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to save deposit QR image")
                toast(R.string.Save_failure)
            }
        }
    }

    private fun saveDepositBitmap(
        bitmap: Bitmap,
        fileName: String,
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return saveBitmapToMediaStore(
                    collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    relativePath = "${Environment.DIRECTORY_DOWNLOADS}${File.separator}Mixin",
                    displayDirPath = getPublicDownloadPath().path,
                    bitmap = bitmap,
                    fileName = fileName,
                )
            } catch (e: Exception) {
                if (isNoSpaceError(e)) throw e
                Timber.w(e, "Failed to save deposit QR image to MediaStore.Downloads")
            }
            try {
                return saveBitmapToMediaStore(
                    collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    relativePath = "${Environment.DIRECTORY_PICTURES}${File.separator}Mixin",
                    displayDirPath = getPublicPicturePath().path,
                    bitmap = bitmap,
                    fileName = fileName,
                )
            } catch (e: Exception) {
                if (isNoSpaceError(e)) throw e
                Timber.w(e, "Failed to save deposit QR image to MediaStore.Images")
            }
            return saveBitmapToAppExternalDir(bitmap, fileName)
        }

        if (hasWritePermission()) {
            try {
                val dir = getPublicDownloadPath()
                if (!dir.exists() && !dir.mkdirs()) {
                    throw IOException("Failed to create directory: ${dir.path}")
                }
                val file = File(dir, fileName)
                try {
                    saveBitmapToFile(file, bitmap)
                } catch (e: Exception) {
                    runCatching { file.deleteOnExists() }
                    throw e
                }
                MediaScannerConnection.scanFile(this@DepositShareActivity, arrayOf(file.toString()), null, null)
                return dir.path
            } catch (e: Exception) {
                if (isNoSpaceError(e)) throw e
                Timber.w(e, "Failed to save deposit QR image to public download dir")
            }
        }
        return saveBitmapToAppExternalDir(bitmap, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveBitmapToMediaStore(
        collectionUri: Uri,
        relativePath: String,
        displayDirPath: String,
        bitmap: Bitmap,
        fileName: String,
    ): String {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val resolver = contentResolver
        val uri =
            resolver.insert(collectionUri, values)
                ?: throw IOException("Failed to insert MediaStore entry in $relativePath")
        try {
            val outputStream =
                resolver.openOutputStream(uri)
                    ?: throw IOException("Failed to open output stream for $uri")
            outputStream.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("Failed to compress bitmap")
                }
                out.flush()
            }
            val doneValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            resolver.update(uri, doneValues, null, null)
            return displayDirPath
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    private fun saveBitmapToAppExternalDir(
        bitmap: Bitmap,
        fileName: String,
    ): String {
        val dir =
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: throw IOException("External files directory unavailable")
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Failed to create directory: ${dir.path}")
        }
        val file = File(dir, fileName)
        try {
            saveBitmapToFile(file, bitmap)
        } catch (e: Exception) {
            runCatching { file.deleteOnExists() }
            throw e
        }
        return dir.path
    }

    private fun isNoSpaceError(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is ErrnoException && current.errno == OsConstants.ENOSPC) {
                return true
            }
            val message = current.message
            if (message != null && (message.contains("ENOSPC", ignoreCase = true) || message.contains("No space left on device", ignoreCase = true))) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw IOException("Failed to compress bitmap")
            }
            out.flush()
        }
    }
}

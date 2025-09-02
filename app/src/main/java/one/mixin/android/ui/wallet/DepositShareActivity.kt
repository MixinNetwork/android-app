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
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Scheme.HTTPS_MARKET
import one.mixin.android.R
import one.mixin.android.databinding.ActivityDepositShareBinding
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.supportsS
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.web.getScreenshot
import one.mixin.android.ui.web.refreshScreenshot
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class DepositShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_TOKEN = "token"
        private const val ARGS_ADDRESS = "address"
        private const val ARGS_AMOUNT = "amount"
        private const val ARGS_AMOUNT_URL = "amount_url"

        private const val ARGS_USER = "user"


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
    }

    override fun getNightThemeId(): Int = R.style.AppTheme_Night_Blur

    override fun getDefaultThemeId(): Int = R.style.AppTheme_Blur

    private lateinit var binding: ActivityDepositShareBinding
    private val token: TokenItem? by lazy {
        intent.extras?.getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepositShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getScreenshot()?.let {
            supportsS({
                binding.background.background = it.toDrawable(resources)
                binding.background.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR))
            }, {
                binding.container.background = it.blurBitmap(25).toDrawable(resources)
            })
        }

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = android.graphics.Color.TRANSPARENT

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
            val qrcodeContent = "$HTTPS_MARKET/${token?.assetId}?ref=$it"
            val qrCode = qrcodeContent.generateQRCode(200.dp, 8.dp).first
            binding.qr.setImageBitmap(qrCode)
        }
        applyFadeInAnimation(binding.root)
    }

    private fun setupUI() {
        val tokenItem = token
        if (user != null || tokenItem ==null) {
            val u = user ?: Session.getAccount()?.toUser()
            binding.titleTv.text = u?.fullName
            binding.subTitleTv.text = getString(R.string.contact_mixin_id, u?.identityNumber ?: "")
            binding.containerLl.isVisible = false
            (amountUrl ?: address)?.let { addr ->
                val qrCode = addr.generateQRCode(120.dp, 8.dp).first
                binding.qrCode.setImageBitmap(qrCode)
                binding.icon.loadImage(u?.avatarUrl)
            }
            binding.bottomTv.isVisible = true
            if (tokenItem != null) {
                binding.bottomTv.setText(getString(R.string.transfer_qrcode_prompt_amount, "$amount"))
            } else {
                binding.bottomTv.setText(getString(R.string.transfer_qrcode_prompt))
            }
        } else {
            binding.titleTv.text = getString(R.string.Deposit_to_Mixin, token?.symbol ?: "")
            binding.subTitleTv.text = getString(R.string.Deposit_to_Mixin_sub, token?.symbol ?: "")
            (amountUrl ?: address)?.let { addr ->
                val qrCode = addr.generateQRCode(120.dp, 8.dp).first
                binding.qrCode.setImageBitmap(qrCode)
                binding.icon.loadImage(token?.iconUrl)
            }

            binding.addressText.text = address ?: ""
            binding.addressTitle.setText(R.string.Address)
            binding.networkText.text = tokenItem.chainName

            if (amount != null) {
                binding.minimumDepositTitle.setText(R.string.Amount)
                binding.minimumDepositText.text = "$amount"
            } else {
                binding.minimumDepositText.text = "${tokenItem.dust} ${tokenItem.symbol}"
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
            val fileName = "${token?.symbol}_deposit.png"
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
            delay(100)
            val bitmap = binding.test.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val fileName = "${token?.symbol}_deposit.png"
            val file = File(dir, fileName)
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(this@DepositShareActivity, arrayOf(file.toString()), null, null)
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

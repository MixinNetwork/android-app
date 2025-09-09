package one.mixin.android.ui.wallet

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
import androidx.core.view.isInvisible
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
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.colorFromAttribute
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

@AndroidEntryPoint
class DepositShareActivity : BaseActivity() {
    companion object {
        private const val ARGS_TOKEN = "token"
        private const val ARGS_WEB3_TOKEN = "web3_token"
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

    private val tokenDust: String?
        get() = token?.dust ?: "0" // Web3TokenItem doesn't have dust, use default

    override fun onCreate(savedInstanceState: Bundle?) {
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
            val qrcodeContent = "$HTTPS_MARKET/${tokenAssetId}?ref=$it"
            val qrCode = qrcodeContent.generateQRCode(200.dp, 8.dp).first
            binding.qr.setImageBitmap(qrCode)
        }
        applyFadeInAnimation(binding.root)
    }

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
                binding.icon.bg.loadUrl(u?.avatarUrl, R.drawable.ic_avatar_place_holder)
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
                binding.icon.bg.loadUrl(tokenIconUrl)
                binding.icon.badge.loadImage(tokenChainUrl, R.drawable.ic_avatar_place_holder)
            }

            val addr = address ?: ""
            if (addr.length > 14) {
                val spannable = android.text.SpannableStringBuilder(addr)
                val black = colorFromAttribute(R.attr.text_primary)
                val gray = colorFromAttribute(R.attr.text_assist)
                val boldStyle = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)

                spannable.setSpan(android.text.style.ForegroundColorSpan(gray), 8, addr.length - 6, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannable.setSpan(android.text.style.ForegroundColorSpan(black), 0, 8, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(boldStyle, 0, 8, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                spannable.setSpan(android.text.style.ForegroundColorSpan(black), addr.length - 6, addr.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), addr.length - 6, addr.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                binding.addressText.text = spannable
            } else {
                binding.addressText.text = addr
            }
            binding.addressTitle.setText(R.string.Address)
            binding.networkText.text = tokenChainName

            if (amount != null) {
                binding.minimumDepositTitle.setText(R.string.Amount)
                binding.minimumDepositText.text = "$amount"
            } else if (token!=null){
                binding.minimumDepositText.text = "${tokenDust} ${tokenSymbol}"
            } else{
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
            delay(100)
            val bitmap = binding.test.drawToBitmap()
            val dir = getPublicDownloadPath()
            dir.mkdirs()
            val fileName = "${tokenSymbol}_deposit.png"
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

package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
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
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.buildReferralCopyUrl
import one.mixin.android.api.referral.buildReferralShareUrl
import one.mixin.android.databinding.FragmentMarketShareBottomBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.round
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.ReferralRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.applyReferralTitleTypeface
import one.mixin.android.ui.common.buildReferralDescription
import one.mixin.android.ui.common.isZeroPercent
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class MarketShareBottomFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MarketShareBottomFragment"
        private const val ARGS_NAME = "name"
        private const val ARGS_COIN = "coin"
        private const val SHARE_QR_URL = "https://mixin.one/mm"

        fun newInstance(
            cover: Bitmap,
            name: String,
            coinId: String,
        ) = MarketShareBottomFragment().withArgs {
            putParcelable(ARGS_COVER, cover)
            putString(ARGS_NAME, name)
            putString(ARGS_COIN, coinId)
        }

        private const val ARGS_COVER = "cover"
    }

    @Inject
    lateinit var referralRepository: ReferralRepository

    private val binding by viewBinding(FragmentMarketShareBottomBinding::inflate)
    private val cover: Bitmap? by lazy {
        arguments?.getParcelable(ARGS_COVER)
    }
    private val name: String? by lazy {
        arguments?.getString(ARGS_NAME)
    }
    private val coinId: String? by lazy {
        arguments?.getString(ARGS_COIN)
    }
    private val referralCode: String?
        get() = referralShareInfo?.code?.takeIf { it.isNotBlank() }

    private var referralShareInfo: ReferralShareInfo? = null
    private var isLoading = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        binding.llMarketShare.round(8.dp)
        bindMixinContact()

        binding.apply {
            close.setOnClickListener { dismiss() }
            share.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.SHARE_IMAGE)
                shareToSystem()
            }
            mixinContact.setOnClickListener {
                if (isLoading) return@setOnClickListener
                shareToMixinContact()
            }
            copy.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.COPY_LINK)
                copyLink()
            }
            save.setOnClickListener {
                if (isLoading) return@setOnClickListener
                AnalyticsTracker.trackMarketDetailShare(AnalyticsTracker.MarketShareType.SAVE_TO_ALBUM)
                saveToAlbum()
            }
        }

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
                    currentQrUrl().generateQRCode(58.dp, qrPadding).first.roundQrBackground(qrPadding, 6.dp.toFloat())
                }
                preparedCover?.let(binding.image::setImageBitmap)
                bindFooter(qrCode)
                bindMixinContact()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.llMarketShare.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.shareCardLoading.isVisible = show
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
                binding.shareDesc.text = buildReferralDescription(requireContext(), rebatePercent)
            }
        } else {
            binding.shareDesc.isVisible = true
            binding.shareDesc.minLines = 1
            binding.title.text = getString(R.string.mixin_messenger)
            binding.shareDesc.text = getString(R.string.share_desc)
        }
        binding.qr.setImageBitmap(qrCode)
    }

    private fun bindMixinContact() {
        val rebatePercent = referralShareInfo?.rebatePercent
        binding.mixinContactDesc.text = if (!rebatePercent.isNullOrBlank() && !rebatePercent.isZeroPercent()) {
            getString(R.string.perps_share_mixin_contact_desc_with_percent, rebatePercent)
        } else {
            getString(R.string.perps_share_mixin_contact_desc)
        }
    }

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

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

    private fun createShareBitmap(): Bitmap = binding.llMarketShare.drawToBitmap()

    private fun shareToSystem() {
        lifecycleScope.launch {
            val file = createShareFile()
            val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", file)
            val share = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
            }
            dismiss()
            startActivity(android.content.Intent.createChooser(share, getString(R.string.Share)))
        }
    }

    private fun shareToMixinContact() {
        lifecycleScope.launch {
            val file = createShareFile()
            val message = ForwardMessage(
                ShareCategory.Image,
                GsonHelper.customGson.toJson(ShareImageData(file.toUri().toString())),
            )
            dismiss()
            ForwardActivity.show(requireContext(), arrayListOf(message), ForwardAction.App.Resultless())
        }
    }

    private suspend fun createShareFile(): File {
        val bitmap = createShareBitmap()
        val file = File(requireContext().cacheDir, "${buildFileName()}.png")
        withContext(Dispatchers.IO) {
            saveBitmapToFile(file, bitmap)
        }
        return file
    }

    private fun copyLink() {
        val marketLink = "$HTTPS_MARKET/$coinId"
        val link = buildReferralCopyUrl(
            referralCode = referralCode,
            defaultUrl = marketLink,
            legacyReferralUrl = Session.getAccount()?.identityNumber?.let { "$marketLink?ref=$it" },
        )
        requireContext().getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        dismiss()
        toast(R.string.copied_to_clipboard)
    }

    private fun saveToAlbum() {
        lifecycleScope.launch {
            delay(100)
            val bitmap = createShareBitmap()
            val dir = requireContext().getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "${buildFileName()}.png")
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(requireContext(), arrayOf(file.toString()), null, null)
            dismiss()
            toast(getString(R.string.Save_to, dir.path))
        }
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun buildFileName(): String = name?.replace("[^A-Za-z0-9._-]".toRegex(), "_") ?: "market"
}

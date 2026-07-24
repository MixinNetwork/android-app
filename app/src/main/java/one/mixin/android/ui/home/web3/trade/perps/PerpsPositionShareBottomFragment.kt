package one.mixin.android.ui.home.web3.trade.perps

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.buildReferralCopyUrl
import one.mixin.android.api.referral.buildReferralShareUrl
import one.mixin.android.api.response.perps.PerpsOrder
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.databinding.FragmentPerpsPositionShareBottomBinding
import one.mixin.android.databinding.ItemPerpsPositionSharePosterBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.getSafeAreaInsetsTop
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.ReferralRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinComposeBottomSheetDialogFragment
import one.mixin.android.ui.common.applyReferralTitleTypeface
import one.mixin.android.ui.common.isZeroPercent
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.math.absoluteValue

@AndroidEntryPoint
class PerpsPositionShareBottomFragment : MixinComposeBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PerpsPositionShareBottomFragment"
        private const val ARGS_POSITION = "args_position"
        private const val ARGS_CLOSE_ORDER = "args_close_order"
        private const val ARGS_CLOSE_LEVERAGE = "args_close_leverage"
        private const val SHARE_QR_URL = "https://mixin.one/mm"
        private const val SHARE_CARD_COVER_URL = "https://dl.mixinpay.com/perps-share-card.png"
        private val MIN_DISPLAY_PNL_PERCENT = BigDecimal("-100")

        fun newInstance(position: PerpsPositionItem) = PerpsPositionShareBottomFragment().withArgs {
            putParcelable(ARGS_POSITION, position)
        }

        fun newInstance(order: PerpsOrderItem, leverage: Int) = PerpsPositionShareBottomFragment().withArgs {
            putParcelable(ARGS_CLOSE_ORDER, order)
            putInt(ARGS_CLOSE_LEVERAGE, leverage)
        }
    }

    @Inject
    lateinit var referralRepository: ReferralRepository

    private val binding by viewBinding(FragmentPerpsPositionShareBottomBinding::inflate)
    private val position: PerpsPositionItem? by lazy {
        arguments?.getParcelableCompat(ARGS_POSITION, PerpsPositionItem::class.java)
    }
    private val closeOrder: PerpsOrderItem? by lazy {
        arguments?.getParcelableCompat(ARGS_CLOSE_ORDER, PerpsOrderItem::class.java)
    }
    private val closeLeverage: Int by lazy {
        arguments?.getInt(ARGS_CLOSE_LEVERAGE, closeOrder?.leverage ?: 0) ?: closeOrder?.leverage ?: 0
    }
    private val quoteColorReversed: Boolean by lazy {
        requireContext().defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    }
    private val referralCode: String?
        get() = referralShareInfo?.code?.takeIf { it.isNotBlank() }

    private var referralShareInfo: ReferralShareInfo? = null
    private var isLoading = false
    private var currentDisplayMetric = ShareDisplayMetric.PNL
    private var currentPosterStyle = SharePosterStyle.CLASSIC
    private lateinit var shareData: ShareCardData
    private lateinit var posterAdapter: PosterAdapter

    override fun getTheme() = R.style.AppTheme_Dialog

    @Composable
    override fun ComposeContent() {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ): View = binding.root.apply {
        roundTopOrBottom(11.dp.toFloat(), top = true, bottom = false)
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!bindContent()) {
            dismiss()
            return
        }
        setupPosterPager()
        bindMixinContact()

        binding.apply {
            close.setOnClickListener { dismiss() }
            share.setOnClickListener {
                if (!isLoading) shareToSystem()
            }
            mixinContact.setOnClickListener {
                if (!isLoading) shareToMixinContact()
            }
            copy.setOnClickListener {
                if (!isLoading) copyLink()
            }
            save.setOnClickListener {
                if (!isLoading) saveToAlbum()
            }
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                if (isLoading) {
                    syncDisplayMetricToggle()
                    return@setOnCheckedChangeListener
                }
                updateDisplayMetric(
                    if (checkedId == R.id.pnl_toggle) {
                        ShareDisplayMetric.PNL
                    } else {
                        ShareDisplayMetric.ROE
                    },
                )
            }
        }

        lifecycleScope.launch {
            showLoading(true)
            try {
                referralShareInfo = withContext(Dispatchers.IO) {
                    referralRepository.fetchDefaultReferralShareInfoOrNull(logLabel = "perps position share")
                }
                bindFooter()
                applyFadeInAnimation(binding.content)
            } finally {
                showLoading(false)
            }
        }
    }

    override fun getBottomSheetHeight(view: View): Int {
        return requireContext().screenHeight() - view.getSafeAreaInsetsTop()
    }

    override fun showError(error: String) {
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.posterPager.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.posterLoading.isVisible = show
    }

    private fun bindContent(): Boolean {
        val open = position
        if (open != null) {
            val pnlAmount = open.unrealizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
            var pnlPercent = (open.roe.toBigDecimalSafely() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
            if (pnlPercent.compareTo(BigDecimal.ZERO) == 0 && pnlAmount.compareTo(BigDecimal.ZERO) != 0) {
                val margin = open.margin.toBigDecimalSafely() ?: BigDecimal.ZERO
                if (margin.compareTo(BigDecimal.ZERO) != 0) {
                    pnlPercent = pnlAmount.divide(margin, 8, RoundingMode.HALF_UP).multiply(BigDecimal(100))
                }
            }
            bindCardData(
                marketId = open.marketId,
                iconUrl = open.iconUrl,
                side = open.side,
                leverage = open.leverage,
                pnlAmount = pnlAmount,
                pnlPercent = pnlPercent,
                tokenSymbol = open.tokenSymbol.orEmpty(),
                displaySymbol = open.displaySymbol.orEmpty(),
                entryPrice = open.entryPrice,
                latestLabel = getString(R.string.perps_current_price),
                latestPrice = open.markPrice ?: open.entryPrice,
            )
            return true
        }

        val closed = closeOrder ?: return false
        if (closed.orderType != PerpsOrder.TYPE_CLOSE) return false
        val pnlAmount = closed.realizedPnl.toBigDecimalSafely() ?: BigDecimal.ZERO
        val effectiveLeverage = if (closed.leverage > 0) closed.leverage else 1
        val pnlPercent = (closed.roe.toBigDecimalSafely() ?: BigDecimal.ZERO).multiply(BigDecimal(100))
        bindCardData(
            marketId = closed.marketId,
            iconUrl = closed.iconUrl,
            side = closed.side,
            leverage = effectiveLeverage,
            pnlAmount = pnlAmount,
            pnlPercent = pnlPercent,
            tokenSymbol = closed.tokenSymbol.orEmpty(),
            displaySymbol = closed.displaySymbol.orEmpty(),
            entryPrice = closed.entryPrice,
            latestLabel = getString(R.string.Close_Price),
            latestPrice = closed.closePrice,
        )
        return true
    }

    private fun bindCardData(
        marketId: String,
        iconUrl: String?,
        side: String,
        leverage: Int,
        pnlAmount: BigDecimal,
        pnlPercent: BigDecimal,
        tokenSymbol: String,
        displaySymbol: String,
        entryPrice: String,
        latestLabel: String,
        latestPrice: String,
    ) {
        shareData = ShareCardData(
            marketId = marketId,
            iconUrl = iconUrl,
            side = side,
            leverage = leverage,
            pnlAmount = pnlAmount,
            pnlPercent = pnlPercent,
            tokenSymbol = tokenSymbol,
            displaySymbol = displaySymbol,
            entryPrice = entryPrice,
            latestLabel = latestLabel,
            latestPrice = latestPrice,
        )
        bindCard()
    }

    private fun bindCard() {
        syncDisplayMetricToggle()
        if (::posterAdapter.isInitialized) {
            refreshVisiblePosters()
        }
    }

    private fun bindFooter() {
        bindMixinContact()
        if (::posterAdapter.isInitialized) {
            refreshVisiblePosters()
        }
    }

    private fun refreshVisiblePosters() {
        val recyclerView = binding.posterPager.getChildAt(0) as? RecyclerView ?: return
        SharePosterStyle.values().forEachIndexed { index, style ->
            (recyclerView.findViewHolderForAdapterPosition(index) as? PosterViewHolder)?.bind(style)
        }
        binding.posterPager.post {
            updatePosterPagerHeight()
        }
    }

    private fun bindMixinContact() {
        val rebatePercent = referralShareInfo?.rebatePercent
        binding.mixinContactDesc.text = if (!rebatePercent.isNullOrBlank() && !rebatePercent.isZeroPercent()) {
            highlightPercent(getString(R.string.perps_share_mixin_contact_desc_with_percent, rebatePercent), rebatePercent)
        } else {
            getString(R.string.perps_share_mixin_contact_desc)
        }
    }

    private fun highlightPercent(text: String, percent: String): CharSequence {
        val start = text.indexOf(percent)
        if (start < 0) return text
        return SpannableString(text).apply {
            val end = start + percent.length
            setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_quote_purple)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun updateDisplayMetric(metric: ShareDisplayMetric) {
        if (currentDisplayMetric == metric) return
        currentDisplayMetric = metric
        bindCard()
    }

    private fun updatePosterStyle(style: SharePosterStyle) {
        if (currentPosterStyle == style) return
        currentPosterStyle = style
    }

    private fun syncDisplayMetricToggle() {
        val checkedId = when (currentDisplayMetric) {
            ShareDisplayMetric.ROE -> R.id.roe_toggle
            ShareDisplayMetric.PNL -> R.id.pnl_toggle
        }
        if (binding.radioGroup.checkedRadioButtonId != checkedId) {
            binding.radioGroup.check(checkedId)
        }
    }

    private fun cardBackground(useProfitStyle: Boolean): Int {
        return if (useProfitStyle) {
            R.drawable.bg_perps_position_share_card_profit
        } else {
            R.drawable.bg_perps_position_share_card_loss
        }
    }

    private fun trendImage(style: SharePosterStyle, isProfit: Boolean): Int {
        return when (style) {
            SharePosterStyle.CLASSIC -> if (isProfit) {
                R.drawable.ic_perps_position_share_profit_classic
            } else {
                R.drawable.ic_perps_position_share_loss_classic
            }
            SharePosterStyle.MINIMAL -> if (isProfit) {
                R.drawable.ic_perps_position_share_profit_minimal
            } else {
                R.drawable.ic_perps_position_share_loss_minimal
            }
        }
    }

    private fun setupPosterPager() {
        posterAdapter = PosterAdapter()
        binding.posterPager.apply {
            adapter = posterAdapter
            offscreenPageLimit = 1
            clipToPadding = false
            clipChildren = false
            setPadding(28.dp, 0, 28.dp, 0)
            (getChildAt(0) as? RecyclerView)?.clipToPadding = false
            setPageTransformer { page, position ->
                val factor = (1f - position.absoluteValue).coerceIn(0f, 1f)
                page.alpha = 0.6f + factor * 0.4f
                val scale = 0.92f + factor * 0.08f
                page.scaleX = scale
                page.scaleY = scale
            }
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updatePosterStyle(SharePosterStyle.values()[position])
                        binding.posterPager.post { updatePosterPagerHeight(force = true) }
                    }
                },
            )
            doOnPreDraw {
                updatePosterPagerHeight(force = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.posterPager.post { updatePosterPagerHeight(force = true) }
    }

    private fun updatePosterPagerHeight(force: Boolean = false) {
        val recyclerView = binding.posterPager.getChildAt(0) as? RecyclerView ?: return
        val itemView = recyclerView.findViewHolderForAdapterPosition(binding.posterPager.currentItem)?.itemView ?: return
        val width = itemView.width.takeIf { it > 0 }
            ?: (binding.posterPager.width - binding.posterPager.paddingStart - binding.posterPager.paddingEnd).takeIf { it > 0 }
            ?: return
        itemView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val measuredHeight = itemView.measuredHeight.takeIf { it > 0 } ?: return
        if (!force && binding.posterPager.layoutParams.height == measuredHeight && binding.posterContainer.layoutParams.height == measuredHeight) return
        binding.posterPager.updateLayoutParams<ViewGroup.LayoutParams> {
            height = measuredHeight
        }
        binding.posterContainer.updateLayoutParams<ViewGroup.LayoutParams> {
            height = measuredHeight
        }
    }

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
        val manager = parentFragmentManager
        dismiss()
        ShareMessageBottomSheetDialogFragment.newInstance(buildPerpsAppCardMessage(), null)
            .show(manager, ShareMessageBottomSheetDialogFragment.TAG)
    }

    private fun buildPerpsAppCardMessage(): ForwardMessage {
        val action = "${Constants.Scheme.HTTPS_TRADE}?type=perps&market=${shareData.marketId}"
        val side = if (shareData.side.equals("long", ignoreCase = true)) getString(R.string.Long) else getString(R.string.Short)
        val market = shareData.displaySymbol.ifBlank { shareData.tokenSymbol }
        val title = getString(R.string.perps_share_card_title, shareData.tokenSymbol)
        val description = buildString {
            append(getString(R.string.perps_share_card_market, market))
            append('\n')
            append(getString(R.string.perps_share_card_side, side, shareData.leverage))
        }.take(128)
        val appCard = AppCardData(
            appId = Constants.RouteConfig.ROUTE_BOT_USER_ID,
            iconUrl = shareData.iconUrl,
            coverUrl = SHARE_CARD_COVER_URL,
            cover = null,
            title = title,
            description = description,
            action = null,
            updatedAt = null,
            shareable = true,
            actions = listOf(
                ActionButtonData(
                    label = getString(R.string.perps_share_card_trade_now),
                    color = "#3D75E3",
                    action = action,
                ),
            ),
        )
        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
    }

    private fun copyLink() {
        val link = buildReferralCopyUrl(
            referralCode = referralCode,
            defaultUrl = SHARE_QR_URL,
            legacyReferralUrl = Session.getAccount()?.identityNumber?.let { "$SHARE_QR_URL&referral=$it" },
        )
        requireContext().getClipboardManager().setPrimaryClip(ClipData.newPlainText(null, link))
        dismiss()
        toast(R.string.copied_to_clipboard)
    }

    private fun saveToAlbum() {
        lifecycleScope.launch {
            val bitmap = createShareBitmap()
            val dir = requireContext().getPublicDownloadPath()
            dir.mkdirs()
            val file = File(dir, "${buildFileName()}_position.png")
            saveBitmapToFile(file, bitmap)
            MediaScannerConnection.scanFile(requireContext(), arrayOf(file.toString()), null, null)
            dismiss()
            toast(getString(R.string.Save_to, dir.path))
        }
    }

    private suspend fun createShareFile(): File {
        val bitmap = createShareBitmap()
        val file = File(requireContext().cacheDir, "${buildFileName()}_position.png")
        withContext(Dispatchers.IO) {
            saveBitmapToFile(file, bitmap)
        }
        return file
    }

    private fun createShareBitmap(): Bitmap = currentPosterView()?.drawToBitmap() ?: binding.posterPager.drawToBitmap()

    private fun currentPosterView(): View? {
        val recyclerView = binding.posterPager.getChildAt(0) as? RecyclerView ?: return null
        return recyclerView.findViewHolderForAdapterPosition(binding.posterPager.currentItem)?.itemView
    }

    private fun saveBitmapToFile(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

    private fun buildFileName(): String {
        val name = position?.displaySymbol
            ?: position?.tokenSymbol
            ?: closeOrder?.displaySymbol
            ?: closeOrder?.tokenSymbol
            ?: "perps"
        return name.replace("[^A-Za-z0-9._-]".toRegex(), "_")
    }

    private fun formatSignedPercent(value: BigDecimal): String {
        val displayValue = value.max(MIN_DISPLAY_PNL_PERCENT)
        val sign = when {
            displayValue > BigDecimal.ZERO -> "+"
            displayValue < BigDecimal.ZERO -> "-"
            else -> ""
        }
        val scaled = displayValue.abs().setScale(2, RoundingMode.FLOOR)
        val number = if (scaled.compareTo(BigDecimal.ZERO) == 0) "0.0" else scaled.stripTrailingZeros().toPlainString()
        return "$sign$number%"
    }

    private fun formatSignedAmount(value: BigDecimal): String {
        val sign = when {
            value > BigDecimal.ZERO -> "+"
            value < BigDecimal.ZERO -> "-"
            else -> ""
        }
        return "$sign$PERPS_USD_SYMBOL${value.abs().setScale(2, RoundingMode.FLOOR).priceFormat()}"
    }

    private fun formatFiat(value: String?): String {
        val price = value.toBigDecimalSafely() ?: BigDecimal.ZERO
        return "$PERPS_USD_SYMBOL${price.priceFormat()}"
    }

    private fun applyFadeInAnimation(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
    }

    private fun String?.toBigDecimalSafely(): BigDecimal? = this?.toBigDecimalOrNull()

    private data class ShareCardData(
        val marketId: String,
        val iconUrl: String?,
        val side: String,
        val leverage: Int,
        val pnlAmount: BigDecimal,
        val pnlPercent: BigDecimal,
        val tokenSymbol: String,
        val displaySymbol: String,
        val entryPrice: String,
        val latestLabel: String,
        val latestPrice: String,
    )

    private enum class ShareDisplayMetric(val labelRes: Int) {
        ROE(R.string.perps_share_roe),
        PNL(R.string.perps_share_pnl),
    }

    private enum class SharePosterStyle {
        CLASSIC,
        MINIMAL,
    }

    private inner class PosterAdapter : RecyclerView.Adapter<PosterViewHolder>() {
        private val styles = SharePosterStyle.values()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
            val itemBinding = ItemPerpsPositionSharePosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PosterViewHolder(itemBinding)
        }

        override fun getItemCount(): Int = styles.size

        override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
            holder.bind(styles[position])
        }
    }

    private inner class PosterViewHolder(
        private val itemBinding: ItemPerpsPositionSharePosterBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(style: SharePosterStyle) {
            val data = shareData
            val isProfit = data.pnlAmount >= BigDecimal.ZERO
            val useProfitStyle = if (quoteColorReversed) !isProfit else isProfit
            itemBinding.root.tag = style.name
            itemBinding.topCard.setBackgroundResource(cardBackground(useProfitStyle))
            itemBinding.assetIcon.loadImage(data.iconUrl, R.drawable.ic_avatar_place_holder)
            itemBinding.pnlTv.text = when (currentDisplayMetric) {
                ShareDisplayMetric.ROE -> formatSignedAmount(data.pnlAmount)
                ShareDisplayMetric.PNL -> formatSignedPercent(data.pnlPercent)
            }
            itemBinding.pnlLabelTv.text = getString(currentDisplayMetric.labelRes)

            val isLong = data.side.equals("long", ignoreCase = true)
            itemBinding.sideTagTv.text = "${if (isLong) getString(R.string.Long) else getString(R.string.Short)} ${data.tokenSymbol}".trim()
            itemBinding.leverageTagTv.text = getString(R.string.Perpetual_Leverage_Format, data.leverage)
            itemBinding.trendImage.setImageResource(trendImage(style, isProfit))
            itemBinding.entryValueTv.text = formatFiat(data.entryPrice)
            itemBinding.latestLabelTv.text = data.latestLabel
            itemBinding.latestValueTv.text = formatFiat(data.latestPrice)
            bindPosterFooter(itemBinding)
        }

        private fun bindPosterFooter(itemBinding: ItemPerpsPositionSharePosterBinding) {
            val info = referralShareInfo
            if (info != null) {
                val rebatePercent = info.rebatePercent
                if (rebatePercent.isNullOrBlank() || rebatePercent.isZeroPercent()) {
                    bindDefaultPosterFooter(itemBinding)
                } else {
                    itemBinding.referralTitle.text = info.code
                    itemBinding.referralTitle.applyReferralTitleTypeface()
                    itemBinding.shareDescTv.isVisible = true
                    itemBinding.shareDescTv.minLines = 1
                    itemBinding.shareDescTv.text = buildPerpsReferralDescription(rebatePercent)
                }
            } else {
                bindDefaultPosterFooter(itemBinding)
            }
            val qrPadding = 8.dp
            val qrSize = 72.dp
            val qrCode = currentQrUrl().generateQRCode(qrSize, qrPadding, outputSize = qrSize).first
                .roundQrBackground(qrPadding, 6.dp.toFloat())
            itemBinding.qr.setImageBitmap(qrCode)
        }
    }

    private fun bindDefaultPosterFooter(itemBinding: ItemPerpsPositionSharePosterBinding) {
        itemBinding.shareDescTv.isVisible = true
        itemBinding.shareDescTv.minLines = 1
        itemBinding.referralTitle.text = getString(R.string.Mixin)
        itemBinding.shareDescTv.text = getString(R.string.perps_share_mixin_contact_desc)
    }

    private fun buildPerpsReferralDescription(rebatePercent: String): CharSequence {
        val text = getString(R.string.referral_share_desc, rebatePercent)
        val start = text.indexOf(rebatePercent)
        if (start < 0) return text
        return SpannableString(text).apply {
            val end = start + rebatePercent.length
            setSpan(
                ForegroundColorSpan(android.graphics.Color.parseColor("#FFEE70")),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

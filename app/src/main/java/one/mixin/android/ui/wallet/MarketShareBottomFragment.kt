package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Scheme.HTTPS_MARKET
import one.mixin.android.R
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.buildReferralCopyUrl
import one.mixin.android.api.referral.buildReferralShareUrl
import one.mixin.android.databinding.FragmentMarketShareBottomBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.round
import one.mixin.android.extension.setQuoteTextWithBackgroud
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.repository.ReferralRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.common.applyReferralTitleTypeface
import one.mixin.android.ui.common.buildReferralDescription
import one.mixin.android.ui.common.isZeroPercent
import one.mixin.android.ui.common.roundQrBackground
import one.mixin.android.ui.common.share.ShareMessageBottomSheetDialogFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ActionButtonData
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
class MarketShareBottomFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "MarketShareBottomFragment"
        private const val ARGS_MARKET = "market"
        private const val ARGS_TYPE = "type"
        private const val SHARE_QR_URL = "https://mixin.one/mm"
        private const val SHARE_CARD_COVER_URL = "https://dl.mixinpay.com/share-market-card.png"

        fun newInstance(
            marketItem: MarketItem,
            selectedType: String,
        ) = MarketShareBottomFragment().withArgs {
            putParcelable(ARGS_MARKET, marketItem)
            putString(ARGS_TYPE, selectedType)
        }
    }

    @Inject
    lateinit var referralRepository: ReferralRepository

    private val binding by viewBinding(FragmentMarketShareBottomBinding::inflate)
    private val marketItem: MarketItem by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_MARKET, MarketItem::class.java))
    }
    private var selectedType: String = "1D"
    private val coinId: String? by lazy {
        marketItem.coinId.ifBlank { marketItem.assetIds?.firstOrNull().orEmpty() }
    }
    private val chartAssetId: String
        get() = marketItem.coinId.ifBlank { marketItem.assetIds?.firstOrNull().orEmpty() }
    private val referralCode: String?
        get() = referralShareInfo?.code?.takeIf { it.isNotBlank() }

    private var referralShareInfo: ReferralShareInfo? = null
    private var isLoading = false
    private var isChartLoading = true
    private var isChartContentSet = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        selectedType = arguments?.getString(ARGS_TYPE) ?: "1D"
        bindMarketCard()
        setupMarketChart()
        setupChartTabs()
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
                val qrCode = withContext(Dispatchers.Default) {
                    val qrPadding = 8.dp
                    currentQrUrl().generateQRCode(58.dp, qrPadding).first.roundQrBackground(qrPadding, 6.dp.toFloat())
                }
                bindFooter(qrCode)
                bindMixinContact()
            } finally {
                updateLoadingState()
            }
        }
    }

    private fun bindMarketCard() {
        binding.assetSymbol.text = marketItem.symbol
        binding.assetRank.isVisible = marketItem.marketCapRank.isNotBlank()
        binding.assetRank.text = "#${marketItem.marketCapRank}"
        binding.icon.loadToken(marketItem)
        binding.priceValue.text = formatMarketPrice(marketItem.currentPrice)
        bindRiseText(
            marketItem.priceChangePercentage24H.toFloatOrNull(),
            useNumberFormat = true,
        )
    }

    private fun setupMarketChart() {
        binding.marketChart.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    private fun setupChartTabs() {
        binding.chartRadio1d.text = getString(R.string.days_count_short, 1)
        binding.chartRadio1w.text = getString(R.string.weeks_count_short, 1)
        binding.chartRadio1m.text = getString(R.string.months_count_short, 1)
        binding.chartRadioYtd.text = getString(R.string.ytd)
        binding.chartRadioAll.text = getString(R.string.All).uppercase()
        binding.chartRadioGroup.check(
            when (selectedType) {
                "1W" -> R.id.chart_radio_1w
                "1M" -> R.id.chart_radio_1m
                "1Y" -> R.id.chart_radio_ytd
                "ALL" -> R.id.chart_radio_all
                else -> R.id.chart_radio_1d
            },
        )
        binding.chartRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val nextType = when (checkedId) {
                R.id.chart_radio_1w -> "1W"
                R.id.chart_radio_1m -> "1M"
                R.id.chart_radio_ytd -> "1Y"
                R.id.chart_radio_all -> "ALL"
                else -> "1D"
            }
            if (selectedType == nextType) return@setOnCheckedChangeListener
            selectedType = nextType
            reloadChartContent()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.marketChart.doOnAttach {
            it.visibility = View.VISIBLE
            setChartContent()
        }
    }

    private fun setChartContent() {
        if (isChartContentSet) return
        isChartContentSet = true

        binding.marketChart.setContent {
            Market(
                selectedType,
                chartAssetId,
                dataChange = { percentageChange ->
                    val displayChange = if (selectedType == "1D") {
                        marketItem.priceChangePercentage24H.toFloatOrNull()
                    } else {
                        percentageChange
                    }
                    bindRiseText(displayChange)
                },
                onHighlightChange = { price, percentageChange ->
                    if (price == null) {
                        binding.priceValue.text = formatMarketPrice(marketItem.currentPrice)
                        val displayChange = if (selectedType == "1D") {
                            marketItem.priceChangePercentage24H.toFloatOrNull()
                        } else {
                            percentageChange
                        }
                        bindRiseText(displayChange)
                    } else {
                        binding.priceValue.text = formatMarketPrice(price)
                        bindRiseText(percentageChange)
                    }
                },
                onLoading = { loading ->
                    isChartLoading = loading
                    updateLoadingState()
                },
                interactive = false,
            )
        }
    }

    private fun reloadChartContent() {
        binding.priceValue.text = formatMarketPrice(marketItem.currentPrice)
        isChartContentSet = false
        setChartContent()
    }

    override fun onDestroyView() {
        isChartContentSet = false
        super.onDestroyView()
    }

    private fun bindRiseText(
        percentageChange: Float?,
        useNumberFormat: Boolean = false,
    ) {
        if (percentageChange == null) {
            binding.priceRise.setQuoteTextWithBackgroud(getString(R.string.N_A))
            return
        }
        val text = if (useNumberFormat) {
            "${BigDecimal(percentageChange.toDouble()).numberFormat2()}%"
        } else {
            String.format("%.2f%%", percentageChange)
        }
        binding.priceRise.setQuoteTextWithBackgroud(text, percentageChange >= 0f)
    }

    private fun updateLoadingState() {
        showLoading(isChartLoading || binding.qr.drawable == null)
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.shareCardLoading.isVisible = show
    }

    private fun bindFooter(qrCode: Bitmap) {
        val info = referralShareInfo
        if (info != null) {
            val rebatePercent = info.rebatePercent
            if (rebatePercent.isNullOrBlank()) {
                binding.shareDesc.isVisible = true
                binding.shareDesc.minLines = 1
                binding.title.text = getString(R.string.mixin_messenger)
                binding.shareDesc.text = getString(R.string.share_desc)
            } else {
                binding.title.text = info.code
                binding.title.applyReferralTitleTypeface()
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

    private fun currentQrUrl(): String = referralCode?.let(::buildReferralShareUrl) ?: SHARE_QR_URL

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
        val manager = parentFragmentManager
        dismiss()
        ShareMessageBottomSheetDialogFragment.newInstance(buildMarketAppCardMessage(), null)
            .show(manager, ShareMessageBottomSheetDialogFragment.TAG)
    }

    private fun buildMarketAppCardMessage(): ForwardMessage {
        val marketLink = "$HTTPS_MARKET/$coinId"
        val referral = Session.getAccount()?.identityNumber
        val buildTradeUrl = { input: String, output: String ->
            "${Constants.Scheme.HTTPS_SWAP}?input=$input&output=$output&referral=$referral"
        }
        val stableAssetId = Constants.AssetId.OMNI_USDT_ASSET_ID
        val targetAssetId = marketItem.assetIds?.firstOrNull().orEmpty()
        val marketCap = runCatching {
            BigDecimal(marketItem.marketCap).multiply(BigDecimal(Fiats.getRate())).numberFormatCompact()
        }.getOrDefault(marketItem.marketCap)
        val price = runCatching {
            BigDecimal(marketItem.currentPrice).multiply(BigDecimal(Fiats.getRate())).priceFormat()
        }.getOrDefault(marketItem.currentPrice)
        val changeText = runCatching {
            "${BigDecimal(marketItem.priceChangePercentage24H).numberFormat2()}%"
        }.getOrDefault("N/A")
        val appCard = AppCardData(
            appId = Constants.RouteConfig.ROUTE_BOT_USER_ID,
            iconUrl = marketItem.iconUrl,
            coverUrl = SHARE_CARD_COVER_URL,
            cover = null,
            title = getString(R.string.market_share_card_title, marketItem.symbol),
            description = buildString {
                append(getString(R.string.market_share_card_asset, marketItem.name, marketItem.symbol))
                append('\n')
                append(getString(R.string.market_share_card_market_cap, Fiats.getSymbol(), marketCap))
                append('\n')
                append(getString(R.string.market_share_card_price, Fiats.getSymbol(), price))
                append('\n')
                append(getString(R.string.market_share_card_price_change, changeText))
            }.take(128),
            action = null,
            updatedAt = null,
            shareable = true,
            actions = listOfNotNull(
                targetAssetId.takeIf { it.isNotBlank() }?.let {
                    ActionButtonData(
                        label = getString(R.string.buy_token, marketItem.symbol),
                        color = "#50BD5C",
                        action = buildTradeUrl(stableAssetId, it),
                    )
                },
                targetAssetId.takeIf { it.isNotBlank() }?.let {
                    ActionButtonData(
                        label = getString(R.string.sell_token, marketItem.symbol),
                        color = "#DB454F",
                        action = buildTradeUrl(it, stableAssetId),
                    )
                },
                ActionButtonData(
                    label = getString(R.string.market_share_card_market_button, marketItem.symbol),
                    color = "#3D75E3",
                    action = buildReferralCopyUrl(
                        referralCode = referralCode,
                        defaultUrl = marketLink,
                        legacyReferralUrl = referral?.let { "$marketLink?ref=$it" },
                    ),
                ),
            ),
        )
        return ForwardMessage(ShareCategory.AppCard, GsonHelper.customGson.toJson(appCard))
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

    private fun buildFileName(): String = marketItem.symbol.replace("[^A-Za-z0-9._-]".toRegex(), "_").ifBlank { "market" }

    private fun formatMarketPrice(price: String): String {
        return runCatching {
            val formattedPrice = BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()
            "${Fiats.getSymbol()}$formattedPrice"
        }.getOrElse {
            getString(R.string.N_A)
        }
    }
}

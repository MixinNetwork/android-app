package one.mixin.android.ui.conversation

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityFilter
import androidx.window.embedding.ActivityRule
import androidx.window.embedding.EmbeddingAspectRatio
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator
import one.mixin.android.R
import one.mixin.android.ui.common.TaskDialogActivity
import one.mixin.android.ui.group.GroupActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.inscription.CollectionActivity
import one.mixin.android.ui.home.inscription.InscriptionActivity
import one.mixin.android.ui.home.web3.trade.SwapActivity
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import one.mixin.android.ui.setting.SettingActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.web3.receive.Web3AddressActivity

object ConversationEmbedding {
    const val EXTRA_CONVERSATION = "embedded_conversation"
    private const val EXTRA_ROUTED = "embedded_conversation_routed"
    private const val SPLIT_MIN_WIDTH_DP = 840
    private const val SPLIT_MIN_HEIGHT_DP = 480
    private const val SPLIT_MIN_SMALLEST_WIDTH_DP = 600

    enum class HomeSection(@StringRes val placeholderRes: Int) {
        CHAT(R.string.Select_conversation),
        WALLET(R.string.Select_asset),
        MARKET(R.string.Select_market_item),
        MORE(R.string.Select_explore_item),
    }

    @Volatile
    internal var homeSection: HomeSection = HomeSection.CHAT

    internal var onHomeSectionChanged: (() -> Unit)? = null

    fun updateHomeSection(section: HomeSection) {
        if (homeSection == section) return
        homeSection = section
        onHomeSectionChanged?.invoke()
    }

    internal fun pairRule(context: Context): SplitPairRule =
        sectionRule(context, "home-chat", true, ConversationActivity::class.java)

    internal fun homePairRules(context: Context): List<SplitPairRule> =
        listOf(
            pairRule(context),
            sectionRule(context, "home-wallet", false, WalletActivity::class.java, Web3AddressActivity::class.java),
            sectionRule(context, "home-market", false, PerpsActivity::class.java),
            sectionRule(
                context,
                "home-more",
                false,
                SettingActivity::class.java,
                WebActivity::class.java,
                GroupActivity::class.java,
                SwapActivity::class.java,
                InscriptionActivity::class.java,
                CollectionActivity::class.java,
            ),
        )

    private fun sectionRule(
        context: Context,
        tag: String,
        clearTop: Boolean,
        vararg secondaries: Class<out Activity>,
    ): SplitPairRule {
        val main = ComponentName(context, MainActivity::class.java)
        return SplitPairRule.Builder(secondaries.map { SplitPairFilter(main, ComponentName(context, it), null) }.toSet())
            .setMinWidthDp(SPLIT_MIN_WIDTH_DP)
            .setMinHeightDp(SPLIT_MIN_HEIGHT_DP)
            .setMinSmallestWidthDp(SPLIT_MIN_SMALLEST_WIDTH_DP)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setDefaultSplitAttributes(attributes())
            .setFinishPrimaryWithSecondary(SplitRule.FinishBehavior.NEVER)
            .setFinishSecondaryWithPrimary(SplitRule.FinishBehavior.ALWAYS)
            .setClearTop(clearTop)
            .setTag(tag)
            .build()
    }

    private fun attributes() = SplitAttributes.Builder()
        // The list pane hosts the home rail, so the primary side is wider than a bare list.
        .setSplitType(SplitAttributes.SplitType.ratio(0.42f))
        .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
        .build()

    fun initialize(context: Context) {
        if (WindowSdkExtensions.getInstance().extensionVersion >= 2) {
            SplitController.getInstance(context).setSplitAttributesCalculator { params ->
                splitAttributes(
                    params.areDefaultConstraintsSatisfied,
                    params.parentWindowMetrics.bounds,
                    params.parentWindowMetrics.density,
                    params.parentWindowLayoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull(),
                    params.parentConfiguration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL,
                    params.defaultSplitAttributes,
                )
            }
        }
        val controller = RuleController.getInstance(context)
        homePairRules(context).forEach { controller.addRule(it) }
        controller.addRule(
            ActivityRule.Builder(setOf(ActivityFilter(ComponentName(context, TaskDialogActivity::class.java), null)))
                .setAlwaysExpand(true)
                .setTag("task-dialog")
                .build(),
        )
        controller.addRule(
            SplitPlaceholderRule.Builder(
                setOf(ActivityFilter(ComponentName(context, MainActivity::class.java), null)),
                Intent(context, ConversationPlaceholderActivity::class.java),
            )
                .setMinWidthDp(SPLIT_MIN_WIDTH_DP)
                .setMinHeightDp(SPLIT_MIN_HEIGHT_DP)
                .setMinSmallestWidthDp(SPLIT_MIN_SMALLEST_WIDTH_DP)
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
                .setDefaultSplitAttributes(attributes())
                .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ADJACENT)
                .setSticky(false)
                .setTag("home-placeholder")
                .build(),
        )
    }

    internal fun splitAttributes(
        constraintsSatisfied: Boolean,
        bounds: Rect,
        density: Float,
        fold: FoldingFeature?,
        rtl: Boolean,
        defaultAttributes: SplitAttributes,
    ): SplitAttributes {
        val expanded = SplitAttributes.Builder().setSplitType(SplitAttributes.SplitType.SPLIT_TYPE_EXPAND).build()
        if (!constraintsSatisfied) return expanded
        if (fold == null || (!fold.isSeparating && fold.occlusionType != FoldingFeature.OcclusionType.FULL)) return defaultAttributes
        if (fold.orientation != FoldingFeature.Orientation.VERTICAL) return expanded
        val leftWidth = fold.bounds.left / density
        val rightWidth = (bounds.width() - fold.bounds.right) / density
        val listWidth = if (rtl) rightWidth else leftWidth
        val detailWidth = if (rtl) leftWidth else rightWidth
        if (listWidth < 320 || detailWidth < 480) return expanded
        return SplitAttributes.Builder()
            .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
            .setSplitType(SplitAttributes.SplitType.SPLIT_TYPE_HINGE)
            .build()
    }

    internal fun conversationIntent(context: Context, extras: Bundle): Intent? {
        val conversationId = extras.getString(ConversationFragment.CONVERSATION_ID)
        val recipientId = extras.getString(ConversationFragment.RECIPIENT_ID)
        if (conversationId.isNullOrBlank() && recipientId.isNullOrBlank()) return null
        val arguments = Bundle().apply {
            listOf(
                ConversationFragment.CONVERSATION_ID,
                ConversationFragment.RECIPIENT_ID,
                ConversationFragment.MESSAGE_ID,
                ConversationFragment.INITIAL_UNREAD_MESSAGE_ID,
                ConversationFragment.KEY_WORD,
                ConversationFragment.START_PARAM,
            ).forEach { key -> extras.getString(key)?.let { putString(key, it) } }
            if (extras.containsKey(ConversationFragment.INITIAL_UNREAD_COUNT)) {
                putInt(ConversationFragment.INITIAL_UNREAD_COUNT, extras.getInt(ConversationFragment.INITIAL_UNREAD_COUNT))
            }
            putBoolean(EXTRA_ROUTED, true)
        }
        return Intent(context, ConversationActivity::class.java).putExtras(arguments)
    }

    internal fun windowMeetsSplitRule(widthDp: Float, heightDp: Float): Boolean =
        widthDp >= SPLIT_MIN_WIDTH_DP && heightDp >= SPLIT_MIN_HEIGHT_DP && minOf(widthDp, heightDp) >= SPLIT_MIN_SMALLEST_WIDTH_DP

    fun routeRootConversation(activity: Activity): Boolean {
        if (!activity.isTaskRoot || activity.intent.getBooleanExtra(EXTRA_ROUTED, false)) return false
        if (SplitController.getInstance(activity).splitSupportStatus != SplitController.SplitSupportStatus.SPLIT_AVAILABLE) return false
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
        if (!windowMeetsSplitRule(metrics.widthDp, metrics.heightDp)) return false
        val extras = activity.intent.extras ?: return false
        extras.putBoolean(EXTRA_ROUTED, true)
        extras.remove(ConversationActivity.ARGS_SHORTCUT)
        activity.startActivity(
            Intent(activity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_CONVERSATION, extras),
        )
        activity.intent.removeExtra(ConversationActivity.ARGS_SHORTCUT)
        activity.finish()
        return true
    }
}

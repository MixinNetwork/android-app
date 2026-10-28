package one.mixin.android.ui.conversation

import android.app.Application
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.window.embedding.SplitRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [35])
class ConversationEmbeddingTest {
    @Test
    fun externalRouteOnlyForwardsConversationArguments() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val extras = Bundle().apply {
            putString(ConversationFragment.CONVERSATION_ID, "conversation")
            putString(ConversationFragment.INITIAL_UNREAD_MESSAGE_ID, "unread")
            putBoolean("args_fast_show", true)
            putString("unrelated", "ignored")
        }
        val intent = requireNotNull(ConversationEmbedding.conversationIntent(context, extras))
        assertEquals("conversation", intent.getStringExtra(ConversationFragment.CONVERSATION_ID))
        assertEquals("unread", intent.getStringExtra(ConversationFragment.INITIAL_UNREAD_MESSAGE_ID))
        assertFalse(intent.hasExtra("args_fast_show"))
        assertFalse(intent.hasExtra("unrelated"))
        assertNull(ConversationEmbedding.conversationIntent(context, Bundle()))
    }

    @Test
    fun splitKeepsTheListWhenDetailClosesAndReplacesPreviousDetail() {
        val rule = ConversationEmbedding.pairRule(ApplicationProvider.getApplicationContext())
        assertEquals(840, rule.minWidthDp)
        assertEquals(480, rule.minHeightDp)
        assertEquals(600, rule.minSmallestWidthDp)
        assertEquals(SplitRule.FinishBehavior.NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(SplitRule.FinishBehavior.ALWAYS, rule.finishSecondaryWithPrimary)
        assertTrue(rule.clearTop)
        assertEquals(1, rule.filters.size)
    }

    @Test
    fun homeTabsSplitTheirOwnDetailsBesideTheList() {
        val rules = ConversationEmbedding.homePairRules(ApplicationProvider.getApplicationContext())
        assertEquals(listOf("home-chat", "home-wallet", "home-market", "home-more"), rules.map { it.tag })
        val secondaryNames = rules.flatMap { rule -> rule.filters.map { it.secondaryActivityName.className } }
        assertEquals(secondaryNames.toSet().size, secondaryNames.size)
        assertTrue(secondaryNames.any { it.endsWith(".ConversationActivity") })
        assertTrue(secondaryNames.any { it.endsWith(".WalletActivity") })
        assertTrue(secondaryNames.any { it.endsWith(".Web3AddressActivity") })
        assertTrue(secondaryNames.any { it.endsWith(".PerpsActivity") })
        assertTrue(secondaryNames.any { it.endsWith(".SettingActivity") })
        assertTrue(secondaryNames.any { it.endsWith(".WebActivity") })
        assertFalse(secondaryNames.any { it.endsWith(".WalletSecurityActivity") })
        assertTrue(rules.first().clearTop)
        assertTrue(rules.drop(1).none { it.clearTop })
        rules.forEach { rule ->
            assertEquals(840, rule.minWidthDp)
            assertEquals(480, rule.minHeightDp)
            assertEquals(600, rule.minSmallestWidthDp)
            assertEquals(SplitRule.FinishBehavior.NEVER, rule.finishPrimaryWithSecondary)
            assertEquals(SplitRule.FinishBehavior.ALWAYS, rule.finishSecondaryWithPrimary)
        }
    }

    @Test
    fun foldedWindowDoesNotMeetTheSplitRule() {
        assertFalse(ConversationEmbedding.windowMeetsSplitRule(360f, 800f))
        assertFalse(ConversationEmbedding.windowMeetsSplitRule(839f, 900f))
        assertFalse(ConversationEmbedding.windowMeetsSplitRule(1200f, 479f))
        assertFalse(ConversationEmbedding.windowMeetsSplitRule(840f, 480f))
        assertFalse(ConversationEmbedding.windowMeetsSplitRule(900f, 500f))
        assertTrue(ConversationEmbedding.windowMeetsSplitRule(840f, 600f))
        assertTrue(ConversationEmbedding.windowMeetsSplitRule(1200f, 800f))
    }
}

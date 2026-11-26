package one.mixin.android.ui.home

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityOneLinkTest {
    @Test
    fun consumeOneLinkIntentIfNeeded_forwardsUrlBeforeClearingData() {
        val url = "https://mixin.onelink.me/rlR3?deep_link_value=referral&deep_link_sub1=CRMLSV"
        val events = mutableListOf<String>()
        var currentData: String? = url

        val consumed = consumeOneLinkIntentIfNeeded(
            action = Intent.ACTION_VIEW,
            dataString = currentData,
            onForwardOneLink = { forwarded ->
                events.add("forward:$forwarded")
                assertEquals(url, currentData)
            },
            onClearData = {
                currentData = null
                events.add("clear")
            },
        )

        assertTrue(consumed)
        assertEquals(listOf("forward:$url", "clear"), events)
        assertEquals(null, currentData)
    }

    @Test
    fun consumeOneLinkIntentIfNeeded_ignoresNonViewOrNonOneLinkIntents() {
        val events = mutableListOf<String>()

        assertFalse(
            consumeOneLinkIntentIfNeeded(
                action = Intent.ACTION_MAIN,
                dataString = "https://mixin.onelink.me/rlR3/shortId",
                onForwardOneLink = { events.add("forward") },
                onClearData = { events.add("clear") },
            ),
        )
        assertFalse(
            consumeOneLinkIntentIfNeeded(
                action = Intent.ACTION_VIEW,
                dataString = "https://mixin.one/referrals/CRMLSV",
                onForwardOneLink = { events.add("forward") },
                onClearData = { events.add("clear") },
            ),
        )
        assertFalse(
            consumeOneLinkIntentIfNeeded(
                action = Intent.ACTION_VIEW,
                dataString = null,
                onForwardOneLink = { events.add("forward") },
                onClearData = { events.add("clear") },
            ),
        )
        assertTrue(events.isEmpty())
    }
}

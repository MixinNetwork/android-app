package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PerpsSharePayloadTest {
    private val json = """{
        "market_id":"e015f42e-b0ff-38e7-87b1-7e8d46fea119","side":"short","leverage":10,
        "entry_price":"100000","mark_price":101250
    }"""

    private fun encode(value: String) = Base64.getEncoder().encodeToString(value.toByteArray())

    @Test
    fun preservesFiveFieldSnapshot() {
        val payload = assertNotNull(PerpsSharePayload.decode(encode(json)))
        assertEquals("e015f42e-b0ff-38e7-87b1-7e8d46fea119", payload.marketId)
        assertEquals("short", payload.side)
        assertEquals(10, payload.leverage)
        assertEquals("100000", payload.entryPrice)
        assertEquals("101250", payload.markPrice)
    }

    @Test
    fun rejectsMalformedDataBeforeMarketLookup() {
        assertNull(PerpsSharePayload.decode(null))
        assertNull(PerpsSharePayload.decode("%%%"))
        for (invalid in listOf("[]", "{}", json.replace("market_id", "id"), json.replace("short", "sell"),
            json.replace(":10,", ":1.5,"), json.replace(":10,", ":\"10\","), json.replace("100000", "0"),
            json.replace("101250", "-1"), """{"position":$json}""")) {
            assertNull(PerpsSharePayload.decode(encode(invalid)))
        }
    }
}

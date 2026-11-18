package one.mixin.android.ui.home.web3.trade.perps

import org.junit.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PerpsSharePayloadTest {
    private val json = """{
        "position":{"market_id":"e015f42e-b0ff-38e7-87b1-7e8d46fea119","side":"short","leverage":10,
            "entry_price":"100000","mark_price":"101250","pnl_amount":"-25.00","pnl_percent":"-12.50"},
        "poster_style":"minimal","display_metric":"pnl"
    }"""

    private fun encode(value: String) = Base64.getEncoder().encodeToString(value.toByteArray())

    @Test
    fun preservesSnapshotAndSelectedPresentation() {
        val payload = assertNotNull(PerpsSharePayload.decode(encode(json)))
        assertEquals("short", payload.side)
        assertEquals("-12.50", payload.pnlPercent.toPlainString())
        assertEquals("-25.00", payload.pnlAmount.toPlainString())
        assertEquals("minimal", payload.posterStyle)
        assertEquals("pnl", payload.displayMetric)
        assertNotNull(PerpsSharePayload.decode(encode(json.replace("minimal", "classic").replace("\"pnl\"", "\"roe\""))))
    }

    @Test
    fun rejectsMalformedDataBeforeMarketLookup() {
        assertNull(PerpsSharePayload.decode(null))
        assertNull(PerpsSharePayload.decode("%%%"))
        for (invalid in listOf("[]", "{}", json.replace("market_id", "id"), json.replace("short", "sell"),
            json.replace("minimal", "auto"), json.replace("-12.50", "NaN"))) {
            assertNull(PerpsSharePayload.decode(encode(invalid)))
        }
    }
}

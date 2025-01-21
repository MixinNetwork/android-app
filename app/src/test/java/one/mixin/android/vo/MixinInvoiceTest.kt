package one.mixin.android.vo

import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigInteger
import java.util.UUID

class MixinInvoiceTest {
    companion object {
        const val BTC = "c6d0c728-2624-429b-8e0d-d9d19b6592fa"
        const val ETH = "43d61dcd-e413-450d-80b8-101d5e903357"
    }

    @Test
    fun testMixinInvoice() {
        val recipient = "MIX4fwusRK88p5GexHWddUQuYJbKMJTAuBvhudgahRXKndvaM8FdPHS2Hgeo7DQxNVoSkKSEDyZeD8TYBhiwiea9PvCzay1A9Vx1C2nugc4iAmhwLGGv4h3GnABeCXHTwWEto9wEe1MWB49jLzy3nuoM81tqE2XnLvUWv"
        val mi = MixinInvoice(
            version = MIXIN_INVOICE_VERSION,
            recipient = recipient.toMixAddress()!!,
            entries = mutableListOf()
        )

        val trace1 = "772e6bef-3bff-4fcc-987d-29bafca74d63"
        val amt1 = "0.12345678".toBigDecimal()
        val ref1 = "7ecf9fc49ff4d2e36424b8e53e67aed8cc4e9d08d7cbdca7d8bdb153ed2fcdde".hexStringToByteArray()
        mi.addEntry(
            traceId = trace1,
            assetId = BTC,
            amount = amt1.toString(),
            extra = "extra one".toByteArray(),
            indexReferences = byteArrayOf(),
            hashReferences = listOf(ref1)
        )

        val trace2 = "3552d116-b29d-4d72-9b24-3ca3b2e0f9c2"
        val amt2 = "0.23345678".toBigDecimal()
        val ref2 = "4a5f79c76872524c6a4a81b174338584e790f09fb059c39cf2a894de1b3c31c6".hexStringToByteArray()
        mi.addEntry(
            traceId = trace2,
            assetId = ETH,
            amount = amt2.toString(),
            extra = "extra two".toByteArray(),
            indexReferences = byteArrayOf(0),
            hashReferences = listOf(ref2)
        )

        val encoded = mi.toString()
        assertEquals(encoded, "MINAABzAgQHZ6h4KBj1RqG2zMcql6d8Q8lKyI9GcTl2tgoJBk8YEejG0McoJiRCm44N2dGbZZL6Z6h4KBj1RqG2zMcql6d8Q8lKyI9GcTl2tgoJBk8YEejG0McoJiRCm44N2dGbZZL6Z6h4KBj1RqG2zMcql6d8QwJ3LmvvO_9PzJh9Kbr8p01jxtDHKCYkQpuODdnRm2WS-gowLjEyMzQ1Njc4AAlleHRyYSBvbmUBAH7Pn8Sf9NLjZCS45T5nrtjMTp0I18vcp9i9sVPtL83eNVLRFrKdTXKbJDyjsuD5wkPWHc3kE0UNgLgQHV6QM1cKMC4yMzM0NTY3OAAJZXh0cmEgdHdvAgEAAEpfecdoclJMakqBsXQzhYTnkPCfsFnDnPKolN4bPDHGTTpvYA")

        val decoded = MixinInvoice.fromString(encoded)
        assertNotNull(decoded)
        assertEquals(MIXIN_INVOICE_VERSION, decoded.version)
        assertEquals(recipient, decoded.recipient.toString())
        assertEquals(2, decoded.entries.size)

        val e1 = decoded.entries[0]
        assertEquals(trace1, e1.traceId)
        assertEquals(BTC, e1.assetId)
        assertEquals(amt1, e1.amount)
        assertEquals("extra one", String(e1.extra))
        assertEquals(0, e1.indexReferences.size)
        assertEquals(1, e1.hashReferences.size)
        assertEquals(ref1.toHex(), e1.hashReferences[0].toHex())

        val e2 = decoded.entries[1]
        assertEquals(trace2, e2.traceId)
        assertEquals(ETH, e2.assetId)
        assertEquals(amt2, e2.amount)
        assertEquals("extra two", String(e2.extra))
        assertEquals(1, e2.indexReferences.size)
        assertEquals(0.toByte(), e2.indexReferences[0])
        assertEquals(1, e2.hashReferences.size)
        assertEquals(ref2.toHex(), e2.hashReferences[0].toHex())
    }
}

fun String.toBigDecimal(): BigInteger {
    return if (contains(".")) {
        val parts = split(".")
        if (parts.size != 2) {
            throw IllegalArgumentException("invalid amount format: $this")
        }
        val intPart = if (parts[0].isEmpty()) "0" else parts[0]
        val decimalPart = parts[1]
        (intPart + decimalPart).toBigInteger()
    } else {
        BigInteger(this)
    }
}

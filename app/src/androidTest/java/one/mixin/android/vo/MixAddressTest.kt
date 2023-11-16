package one.mixin.android.vo

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MixAddressTest {
    @Test
    fun testUuidMixAddress() {
        var members = listOf("67a87828-18f5-46a1-b6cc-c72a97a77c43")
        var ma = MixAddress.newUuidMixAddress(members, 1)
        assertEquals("MIX3QEeg1WkLrjvjxyMQf6Xc8dxs81tpPc", ma.toString())
        var ma1 = "MIX3QEeg1WkLrjvjxyMQf6Xc8dxs81tpPc".toMixAddress()
        assert(ma1 != null)
        assertEquals(ma1!!.members(), members)
        assertEquals(ma1.version, 0x2.toByte())
        assertEquals(ma1.threshold, 0x1.toByte())

        members = listOf(
            "67a87828-18f5-46a1-b6cc-c72a97a77c43",
            "c94ac88f-4671-3976-b60a-09064f1811e8",
            "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
            "67a87828-18f5-46a1-b6cc-c72a97a77c43",
            "c94ac88f-4671-3976-b60a-09064f1811e8",
            "c6d0c728-2624-429b-8e0d-d9d19b6592fa",
            "67a87828-18f5-46a1-b6cc-c72a97a77c43",
        )
        ma = MixAddress.newUuidMixAddress(members, 4)
        assertEquals("MIX4fwusRK88p5GexHWddUQuYJbKMJTAuBvhudgahRXKndvaM8FdPHS2Hgeo7DQxNVoSkKSEDyZeD8TYBhiwiea9PvCzay1A9Vx1C2nugc4iAmhwLGGv4h3GnABeCXHTwWEto9wEe1MWB49jLzy3nuoM81tqE2XnLvUWv", ma.toString())
        ma1 = "MIX4fwusRK88p5GexHWddUQuYJbKMJTAuBvhudgahRXKndvaM8FdPHS2Hgeo7DQxNVoSkKSEDyZeD8TYBhiwiea9PvCzay1A9Vx1C2nugc4iAmhwLGGv4h3GnABeCXHTwWEto9wEe1MWB49jLzy3nuoM81tqE2XnLvUWv".toMixAddress()
        assert(ma1 != null)
        assertEquals(ma1!!.members(), members)
        assertEquals(ma1.version, 0x2.toByte())
        assertEquals(ma1.threshold, 0x4.toByte())
    }

    @Test
    fun testMainnetMixAddress() {
        var members = listOf("XIN3BMNy9pQyj5XWDJtTbaBVE2zQ66zBo2weyc43iL286asdqwApWswAzQC5qba26fh3fzHK9iMoxyx1q3Lgj45KJftzGD9q")
        var ma = MixAddress.newMainnetMixAddress(members, 1)
        assertEquals("MIXPYWwhjxKsbFRzAP2Dcb2mMjj7sQQo4MpCSv3NYaYCdQ2kEcbcimpPT81gaxtuNhunLWPx7Sv7fawjZ8DhRmEj8E2hrQM4Z6e", ma.toString())
        var ma1 = "MIXPYWwhjxKsbFRzAP2Dcb2mMjj7sQQo4MpCSv3NYaYCdQ2kEcbcimpPT81gaxtuNhunLWPx7Sv7fawjZ8DhRmEj8E2hrQM4Z6e".toMixAddress()
        assert(ma1 != null)
        assertEquals(ma1!!.members(), members)
        assertEquals(ma1.version, 0x2.toByte())
        assertEquals(ma1.threshold, 0x1.toByte())

        members = listOf(
            "XINGNzunRUMmKGqDhnf1MT8tR7ek6ozg2V6dXFHCCg3tndnSRcAdzET8Fw4ktcQKshzteDmyV2RE8aFiKPz8ewrvsj3s7fvC",
            "XINMd9kCbxEoEetZuDM8gGJS11X3TVrRLwzhnqgMr65qjJBkCncNqSAngESpC7Hddnsw1D9Jo2QJakbFPr8WyrM6VkskGkB8",
            "XINLM7VuMYSjvKiEQPyLpaG7NDLDPngWWFBZpVJjhGamMsgPbmeSsGs3fQzNoqSr6syBTyLM3i69T7iSN8Tru7aQadiKLkSV",
        )
        ma = MixAddress.newMainnetMixAddress(members, 2)
        assertEquals("MIXBCirWksVv9nuphqbtNRZZvwKsXHHMUnB5hVrVY1P7f4eBdLpDoLwiQoHYPvXia2wFepnX6hJwTjHybzBiroWVEMaFHeRFfLpcU244tzRM8smak9iRAD4PJRHN1MLHRWFtErottp9t7piaRVZBzsQXpSsaSgagj93voQdUuXhuQGZNj3Fme5YYMHfJBWjoRFHis4mnhBgxkyEGRUHAVYnfej2FhrypJmMDu74irRTdj2xjQYr6ovBJSUBYDBcvAyLPE3cEKc4JsPz7b9", ma.toString())
        ma1 = "MIXBCirWksVv9nuphqbtNRZZvwKsXHHMUnB5hVrVY1P7f4eBdLpDoLwiQoHYPvXia2wFepnX6hJwTjHybzBiroWVEMaFHeRFfLpcU244tzRM8smak9iRAD4PJRHN1MLHRWFtErottp9t7piaRVZBzsQXpSsaSgagj93voQdUuXhuQGZNj3Fme5YYMHfJBWjoRFHis4mnhBgxkyEGRUHAVYnfej2FhrypJmMDu74irRTdj2xjQYr6ovBJSUBYDBcvAyLPE3cEKc4JsPz7b9".toMixAddress()
        assert(ma1 != null)
        assertEquals(ma1!!.members(), members)
        assertEquals(ma1.version, 0x2.toByte())
        assertEquals(ma1.threshold, 0x2.toByte())
    }
}


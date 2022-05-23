package one.mixin.android.widget

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PercentViewTest {
    @Test
    fun testCalcPercent() {
        val totalUSD1 = BigDecimal("100")
        val list1 = listOf("100").map {
            BigDecimal(it).calcPercent(totalUSD1)
        }
        assertEquals(list1[0], 1f)

        val totalUSD2 = BigDecimal("100")
        val list2 = listOf("90", "10").map {
            BigDecimal(it).calcPercent(totalUSD2)
        }
        assertEquals(list2[0], 0.9f)
        assertEquals(list2[1], 0.1f)

        val l3 = listOf(0.05, 0.03, 0.02, 0.015, 0.005)
        val totalUSD3 = BigDecimal(l3.sum())
        val list3 = l3.map {
            BigDecimal(it).calcPercent(totalUSD3)
        }
        assertEquals(list3, listOf(0.41f, 0.25f, 0.16f, 0.12f, 0.04f))

        val l5 = listOf(100.0, 44.0, 0.1, 0.2)
        val totalUSD5 = BigDecimal(l5.sum())
        val list5 = l5.map {
            BigDecimal(it).calcPercent(totalUSD5)
        }
        assertEquals(list5, listOf(0.69f, 0.3f, 0f, 0f))
    }
}

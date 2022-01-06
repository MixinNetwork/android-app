package one.mixin.android.util

import org.junit.Test
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

class TimeTest {
    private val localeZone by lazy {
        ZoneId.systemDefault()
    }

    @Test
    fun timeTest(){
        val time = "2022-01-06 05:20:51.101"
        val timeFormat : DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS")
        println(ZonedDateTime.now().format(timeFormat))
        val result = ZonedDateTime.parse(time, timeFormat)
        println(result)
    }
}
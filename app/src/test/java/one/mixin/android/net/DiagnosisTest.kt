package one.mixin.android.net

import kotlin.test.Test

class DiagnosisTest {

    @Test
    fun testPing() {
        ping("google.com")
        ping("baidu.com")
    }
}

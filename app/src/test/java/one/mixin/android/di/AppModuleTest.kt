package one.mixin.android.di

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppModuleTest {
    @Test
    fun referralAcceptLanguageUsesCompleteLanguageTag() {
        assertEquals("zh-CN", referralAcceptLanguage(Locale.SIMPLIFIED_CHINESE))
        assertEquals("zh-TW", referralAcceptLanguage(Locale.TRADITIONAL_CHINESE))
        assertEquals("en-US", referralAcceptLanguage(Locale.US))
    }
}

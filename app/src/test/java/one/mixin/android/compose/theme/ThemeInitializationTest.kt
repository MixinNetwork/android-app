package one.mixin.android.compose.theme

import org.junit.Test

class ThemeInitializationTest {
    @Test
    fun initializesWithoutApplicationContext() {
        Class.forName("one.mixin.android.compose.theme.ThemeKt")
    }
}

package one.mixin.android.compose.theme

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [29])
class ThemeModeTest {
    @Test
    fun runtimeRespectsAppPreferenceAndPreviewRespectsConfiguration() {
        for (systemDark in listOf(false, true)) {
            RuntimeEnvironment.setQualifiers(if (systemDark) "night" else "notnight")
            for (themeId in listOf(Constants.Theme.THEME_DEFAULT_ID, Constants.Theme.THEME_NIGHT_ID, Constants.Theme.THEME_AUTO_ID)) {
                for (preview in listOf(false, true)) {
                    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
                    val activity = controller.get()
                    val view = ComposeView(activity)
                    try {
                        activity.defaultSharedPreferences.edit()
                            .putInt(Constants.Theme.THEME_CURRENT_ID, themeId)
                            .commit()
                        var textColor: Color? = null
                        view.setContent {
                            CompositionLocalProvider(LocalInspectionMode provides preview) {
                                MixinAppTheme {
                                    textColor = MixinAppTheme.colors.textPrimary
                                }
                            }
                        }
                        activity.setContentView(view)
                        shadowOf(Looper.getMainLooper()).idle()

                        val expectedDark = if (preview || themeId == Constants.Theme.THEME_AUTO_ID) {
                            systemDark
                        } else {
                            themeId == Constants.Theme.THEME_NIGHT_ID
                        }
                        assertEquals(
                            "systemDark=$systemDark, themeId=$themeId, preview=$preview",
                            if (expectedDark) Color.White else Color.Black,
                            textColor,
                        )
                    } finally {
                        view.disposeComposition()
                        controller.pause().stop().destroy()
                    }
                }
            }
        }
    }
}

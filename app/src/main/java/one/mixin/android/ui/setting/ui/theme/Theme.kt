package one.mixin.android.ui.setting.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.isNightMode

class AppColors(
    val primary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSubtitle: Color,
    val textMinor: Color,
    val textBlue: Color = Color(0xFF3D75E3),
    val icon: Color,
    val iconGray: Color,
    val backgroundWindow: Color,
    val background: Color,
    val backgroundDark: Color,
    val backgroundGray: Color,
    val red: Color = Color(0xFFE55541),
    val tipError: Color = Color(0xFFF67070),
    val shadow: Color = Color(0x33AAAAAA),
    val unchecked: Color,
    val tipWarning: Color,
    val tipWarningBorder: Color,
)

class AppDrawables(
    @DrawableRes
    val emergencyAvatar: Int,
    @DrawableRes
    val emergencyContact: Int,
)

object MixinAppTheme {
    val colors: AppColors
        @Composable
        get() = LocalColors.current

    val drawables: AppDrawables
        @Composable
        get() = LocalDrawables.current
}

private val LightColorPalette =
    AppColors(
        primary = Color(0xFFFFFFFF),
        accent = Color(0xFF3D75E3),
        textPrimary = Color(0xFF333333),
        textSubtitle = Color(0xFFBBBEC3),
        textMinor = Color(0xFFBBBBBB),
        icon = Color(0xFF000000),
        iconGray = Color(0xFFD2D4DA),
        backgroundWindow = Color(0xFFF6F7FA),
        background = Color(0xFFFFFFFF),
        backgroundDark = Color(0xFF999999),
        backgroundGray = Color(0xFFF5F7FA),
        unchecked = Color(0xFFECECEC),
        tipWarning = Color(0xFFFBF1F0),
        tipWarningBorder = Color(0xFFE86B67)
    )

private val DarkColorPalette =
    AppColors(
        primary = Color(0xFF2c3136),
        accent = Color(0xFF3D75E3),
        textPrimary = Color(0xE6FFFFFF),
        textSubtitle = Color(0x66FFFFFF),
        textMinor = Color(0xAAD8D8D8),
        icon = Color(0xFFEAEAEB),
        iconGray = Color(0xFF808691),
        backgroundWindow = Color(0xFF23272B),
        background = Color(0xFF2c3136),
        backgroundDark = Color(0xFF121212),
        backgroundGray = Color(0xFF3B3F44),
        unchecked = Color(0xFFECECEC),
        tipWarning = Color(0xFF3E373B),
        tipWarningBorder = Color(0xFFE86B67)
    )

private val LightDrawablePalette =
    AppDrawables(
        emergencyAvatar = R.drawable.ic_emergency_avatar,
        emergencyContact = R.drawable.ic_emergency_contact,
    )
private val DarkDrawablePalette =
    AppDrawables(
        emergencyAvatar = R.drawable.ic_emergency_avatar_night,
        emergencyContact = R.drawable.ic_emergency_contact_night,
    )

private val LocalColors = compositionLocalOf { LightColorPalette }
private val LocalDrawables = compositionLocalOf { LightDrawablePalette }

@Composable
fun MixinAppTheme(
    darkTheme: Boolean = MixinApplication.get().isNightMode(),
    content: @Composable () -> Unit,
) {
    val colors =
        if (darkTheme) {
            DarkColorPalette
        } else {
            LightColorPalette
        }
    val drawables =
        if (darkTheme) {
            DarkDrawablePalette
        } else {
            LightDrawablePalette
        }
    MaterialTheme(if (darkTheme) darkColors() else lightColors()) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalDrawables provides drawables,
            content = content,
        )
    }
}

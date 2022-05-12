package one.mixin.android.ui.setting.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import one.mixin.android.R


class AppColors(
    val primary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSubtitle: Color,

    val icon: Color,
    val iconGray: Color,

    val backgroundWindow: Color,
    val background: Color,
    val red: Color = Color(0xFFE55541),
)

class AppDrawables(
    @DrawableRes
    val emergencyAvatar: Int,
)

object MixinAppTheme {
    val colors: AppColors
        @Composable
        get() = LocalColors.current

    val drawables: AppDrawables
        @Composable
        get() = LocalDrawables.current

}

private val LightColorPalette = AppColors(
    primary = Color(0xFFFFFFFF),
    accent = Color(0xFF3D75E3),
    textPrimary = Color(0xFF333333),
    textSubtitle = Color(0xFFBBBEC3),
    icon = Color(0xFF000000),
    iconGray = Color(0xFFD2D4DA),
    backgroundWindow = Color(0xFFF6F7FA),
    background = Color(0xFFFFFFFF),
)

private val DarkColorPalette = AppColors(
    primary = Color(0xFF2c3136),
    accent = Color(0xFF3D75E3),
    textPrimary = Color(0xE6FFFFFF),
    textSubtitle = Color(0x66FFFFFF),
    icon = Color(0xFFEAEAEB),
    iconGray = Color(0xFF808691),
    backgroundWindow = Color(0xFF23272B),
    background = Color(0xFF2c3136),
)

private val LightDrawablePalette = AppDrawables(
    emergencyAvatar = R.drawable.ic_emergency_avatar,
)
private val DarkDrawablePalette = AppDrawables(
    emergencyAvatar = R.drawable.ic_emergency_avatar_night,
)

private val LocalColors = compositionLocalOf { LightColorPalette }
private val LocalDrawables = compositionLocalOf { LightDrawablePalette }

@Composable
fun MixinAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }
    val drawables = if (darkTheme) {
        DarkDrawablePalette
    } else {
        LightDrawablePalette
    }
    MaterialTheme(if (darkTheme) darkColors() else lightColors()) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalDrawables provides drawables,
            content = content
        )
    }
}
package one.mixin.android.compose.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
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
    val textAssist: Color,
    val textMinor: Color,
    val textRemarks: Color,
    val textBlue: Color = Color(0xFF3D75E3),
    val icon: Color,
    val iconGray: Color,
    val backgroundWindow: Color,
    val background: Color,
    val backgroundDark: Color,
    val backgroundGrayLight: Color,
    val backgroundGray: Color,
    val red: Color = Color(0xFFE55541),
    val green: Color = Color(0xFF50BD5C),
    val tipError: Color = Color(0xFFF67070),
    val walletRed: Color = Color(0xFFF67070),
    val walletGreen: Color = Color(0xFF50BD5C),
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
        textPrimary = Color(0xFF000000),
        textMinor = Color(0xFF333333),
        textAssist = Color(0xFF888888),
        textRemarks = Color(0xFFB3B3B3),
        icon = Color(0xFF000000),
        iconGray = Color(0xFFD2D4DA),
        backgroundWindow = Color(0xFFF6F7FA),
        background = Color(0xFFFFFFFF),
        backgroundDark = Color(0xFF999999),
        backgroundGrayLight = Color(0xFFF5F7FA),
        backgroundGray = Color(0xFFE5E5E5),
        unchecked = Color(0xFFECECEC),
        tipWarning = Color(0xFFFBF1F0),
        tipWarningBorder = Color(0xFFE86B67),
    )

private val DarkColorPalette =
    AppColors(
        primary = Color(0xFF2c3136),
        accent = Color(0xFF3D75E3),
        textPrimary = Color(0xFFFFFFFF),
        textAssist = Color(0xFF7F878F),
        textMinor = Color(0xFFD3D4D5),
        textRemarks = Color(0xFF6E7073),
        icon = Color(0xFFEAEAEB),
        iconGray = Color(0xFF808691),
        backgroundWindow = Color(0xFF23272B),
        background = Color(0xFF2c3136),
        backgroundDark = Color(0xFF121212),
        backgroundGrayLight = Color(0xFF3B3F44),
        backgroundGray = Color(0xFF3B3F44),
        unchecked = Color(0xFFECECEC),
        tipWarning = Color(0xFF3E373B),
        tipWarningBorder = Color(0xFFE86B67),
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
    val textSelectionColors =
        TextSelectionColors(
            handleColor = Color(0xFF3D75E3),
            backgroundColor = Color(0x663D75E3),
        )
    MaterialTheme(if (darkTheme) darkColors() else lightColors()) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalDrawables provides drawables,
            LocalTextSelectionColors provides textSelectionColors,
            content = content,
        )
    }
}

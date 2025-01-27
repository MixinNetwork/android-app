package one.mixin.android.compose.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.platform.LocalContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isScreenWideColorGamut
import one.mixin.android.util.isCurrChinese
import java.util.Locale

val isP3Supported = MixinApplication.appContext.isScreenWideColorGamut()

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
    val marketRed: Color = if (isP3Supported) Color(
        colorSpace = ColorSpaces.DisplayP3,
        red = 0.898f,
        green = 0.471f,
        blue = 0.455f,
        alpha = 1f
    ) else Color(0xFFE57874),
    val marketGreen: Color = if (isP3Supported) Color(
        colorSpace = ColorSpaces.DisplayP3,
        red = 0.314f,
        green = 0.741f,
        blue = 0.361f,
        alpha = 1f
    ) else Color(0xFF50BD5C),
    val shadow: Color = Color(0x33AAAAAA),
    val unchecked: Color,
    val tipWarning: Color,
    val tipWarningBorder: Color,
    val borderPrimary: Color,
    val rippleColor: Color = Color(0x33000000),
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
    val borderColor: Color,
)

class AppDrawables(
    @DrawableRes
    val bgAlertCard: Int,

    )

object MixinAppTheme {
    val colors: AppColors
        @Composable
        get() = LocalColors.current

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
        borderPrimary = Color(0xFFE5E8EE),
        bgGradientStart = Color(0xFFFFFFFF),
        bgGradientEnd = Color(0xFFE7EFFF),
        borderColor = Color(0xFFE5E8EE),
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
        borderPrimary = Color(0x33FFFFFF),
        bgGradientStart = Color(0xFF2C3136),
        bgGradientEnd = Color(0xFF1C2029),
        borderColor = Color(0xFF6E7073),
    )

private val LocalColors = compositionLocalOf { LightColorPalette }

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
    val textSelectionColors =
        TextSelectionColors(
            handleColor = Color(0xFF3D75E3),
            backgroundColor = Color(0x663D75E3),
        )

    @OptIn(ExperimentalMaterialApi::class)
    val rippleConfiguration = RippleConfiguration(
        color = if (darkTheme) Color.White else Color.LightGray,
        rippleAlpha = RippleDefaults.rippleAlpha(if (darkTheme) Color.White else Color.LightGray, !darkTheme),
    )

    @OptIn(ExperimentalMaterialApi::class)
    MaterialTheme(
        if (darkTheme) darkColors() else lightColors(),
    ) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalTextSelectionColors provides textSelectionColors,
            LocalRippleConfiguration provides rippleConfiguration,
            content = content,
        )
    }
}

@Composable
@DrawableRes
fun languageBasedImage(@DrawableRes defaultImage:Int, @DrawableRes zh:Int) : Int{
    val context = LocalContext.current

    val drawableRes = when {
        isCurrChinese() -> zh
        else -> defaultImage
    }
    return drawableRes
}
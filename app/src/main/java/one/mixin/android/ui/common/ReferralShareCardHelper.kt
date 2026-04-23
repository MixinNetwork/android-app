package one.mixin.android.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import one.mixin.android.R
import java.math.BigDecimal

private const val REFERRAL_REBATE_COLOR = "#FFEE70"

fun buildReferralDescription(context: Context, rebatePercent: String): CharSequence {
    if (rebatePercent.isZeroPercent()) {
        return context.getString(R.string.referral_share_desc_zero)
    }

    val text = context.getString(R.string.referral_share_desc, rebatePercent)
    val start = text.indexOf(rebatePercent)
    return SpannableString(text).apply {
        if (start >= 0) {
            setSpan(
                ForegroundColorSpan(Color.parseColor(REFERRAL_REBATE_COLOR)),
                start,
                start + rebatePercent.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                start + rebatePercent.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
}

fun String.isZeroPercent(): Boolean {
    val normalized = removeSuffix("%").trim()
    return normalized.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) == 0
}

fun Bitmap.roundQrBackground(padding: Int, radius: Float): Bitmap {
    if (width <= 0 || height <= 0 || radius <= 0f) return this

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(this@roundQrBackground, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val inset = padding / 2f
    canvas.drawRoundRect(
        RectF(inset, inset, width - inset, height - inset),
        radius,
        radius,
        paint,
    )
    return output
}

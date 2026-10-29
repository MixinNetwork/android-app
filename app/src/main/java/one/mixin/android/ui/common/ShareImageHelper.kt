package one.mixin.android.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.colorAttr

internal fun View.drawShareBitmap(): Bitmap {
    val (targetWidth, targetHeight) = shareImageSize(width, height)
    return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it)
        canvas.scale(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
        canvas.translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(canvas)
    }
}

internal fun View.drawShareCover(): Bitmap {
    val (targetWidth, targetHeight) = shareImageSize(width, height)
    return Bitmap.createBitmap(SHARE_IMAGE_SIZE, SHARE_IMAGE_SIZE, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it)
        canvas.drawColor(context.colorAttr(R.attr.bg_white))
        canvas.translate((SHARE_IMAGE_SIZE - targetWidth) / 2f, (SHARE_IMAGE_SIZE - targetHeight) / 2f)
        canvas.scale(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
        canvas.translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(canvas)
    }
}

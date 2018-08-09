package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import java.security.MessageDigest

class CropTransformation constructor(private var width: Int, private var height: Int, private var cropType: CropType = CropType.CENTER) : BitmapTransformation() {

    enum class CropType {
        TOP,
        CENTER,
        BOTTOM
    }

    override fun transform(
        context: Context,
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {

        width = if (width == 0) toTransform.width else width
        height = if (height == 0) toTransform.height else height

        val config = if (toTransform.config != null) toTransform.config else Bitmap.Config.ARGB_8888
        val bitmap = pool.get(width, height, config)

        bitmap.setHasAlpha(true)

        val scaleX = width.toFloat() / toTransform.width
        val scaleY = height.toFloat() / toTransform.height
        val scale = Math.max(scaleX, scaleY)

        val scaledWidth = scale * toTransform.width
        val scaledHeight = scale * toTransform.height
        val left = (width - scaledWidth) / 2
        val top = getTop(scaledHeight)
        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(toTransform, null, targetRect, null)

        return bitmap
    }

    private fun getTop(scaledHeight: Float): Float {
        return when (cropType) {
            CropTransformation.CropType.TOP -> 0f
            CropTransformation.CropType.CENTER -> (height - scaledHeight) / 2
            CropTransformation.CropType.BOTTOM -> height - scaledHeight
        }
    }

    override fun toString(): String {
        return "CropTransformation(width=$width, height=$height, cropType=$cropType)"
    }

    override fun equals(o: Any?): Boolean {
        return o is CropTransformation &&
            o.width == width &&
            o.height == height &&
            o.cropType == cropType
    }

    override fun hashCode(): Int {
        return ID.hashCode() + width * 100000 + height * 1000 + cropType.ordinal * 10
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + width + height + cropType).toByteArray())
    }

    companion object {

        private val VERSION = 1
        private val ID = "CropTransformation.$VERSION"
    }
}
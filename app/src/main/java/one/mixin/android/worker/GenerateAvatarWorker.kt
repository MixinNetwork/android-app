package one.mixin.android.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.text.TextPaint
import androidx.collection.ArrayMap
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.R
import one.mixin.android.extension.CodeType
import one.mixin.android.extension.dp
import one.mixin.android.extension.getColorCode
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.saveGroupAvatar
import one.mixin.android.vo.User
import one.mixin.android.widget.AvatarView
import one.mixin.android.widget.drawAvatarText
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class GenerateAvatarWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
    ) : AvatarWorker(context, parameters) {
        private lateinit var texts: ArrayMap<Int, String>
        private val size = 256

        override suspend fun onRun(): Result {
            val groupId = inputData.getString(GROUP_ID) ?: return Result.failure()
            if (conversationDao.getGroupIconUrl(groupId)?.isWebUrl() == true) return Result.success()
            val triple = checkGroupAvatar(groupId)
            if (triple.first) {
                return Result.success()
            }
            val f = triple.third
            val icon = conversationDao.getGroupIconUrl(groupId)
            texts = ArrayMap()
            val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(result)
            val bitmaps = mutableListOf<Bitmap>()
            try {
                getBitmaps(bitmaps, users)
            } catch (e: Exception) {
                return Result.success()
            }
            drawInternal(c, bitmaps)
            result.saveGroupAvatar(applicationContext, triple.second)
            if (icon != null && icon != f.absolutePath) {
                try {
                    File(icon).delete()
                } catch (e: Exception) {
                }
            }
            conversationDao.updateGeneratedGroupIconUrl(groupId, f.absolutePath)
            return Result.success()
        }

        private fun drawInternal(
            canvas: Canvas,
            bitmaps: List<Bitmap>,
        ) {
            // Bitmap text uses pixel proportions, independent of display density and font scale.
            val textSizeLarge = size * 0.4f
            val textSizeMedium = size * 0.33f
            val textSizeSmall = size * 0.26f
            val textOffset = size * 0.06f
            val dividerOffset = 0.5f.dp.toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                }
            val c0 = Color.parseColor("#33FFFFFF") // 20%
            val c1 = Color.parseColor("#E6FFFFFF") // 90%
            val verticalDividerPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, 0f, size / 2f, c0, c1, Shader.TileMode.MIRROR)
                }
            val horizontalDividerPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, size / 2f, 0f, c0, c1, Shader.TileMode.MIRROR)
                }
            val halfHorizontalDividerPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(size / 2f, 0f, size.toFloat(), 0f, c1, c0, Shader.TileMode.CLAMP)
                }
            val verticalDividerRectF = RectF(size / 2f - dividerOffset, 0f, size / 2f + dividerOffset, size.toFloat())
            val horizontalDividerRectF = RectF(0f, size / 2f - dividerOffset, size.toFloat(), size / 2f + dividerOffset)
            val halfHorizontalDividerRectF =
                RectF(
                    size / 2f,
                    size / 2f - dividerOffset,
                    size.toFloat(),
                    size / 2f + dividerOffset,
                )
            val rectF = RectF()

            when (bitmaps.size) {
                1 -> {
                    val b = bitmaps[0]
                    val m = Matrix()
                    val src = RectF(0f, 0f, b.width.toFloat(), b.height.toFloat())
                    val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
                    setPaintAndRect(b, m, src, dst, paint, rectF)
                    canvas.drawArc(rectF, 0f, 360f, true, paint)

                    if (texts[0] != null) {
                        textPaint.textSize = textSizeLarge
                        val t = texts[0].toString()
                        canvas.drawAvatarText(t, textPaint, size / 2f, size / 2f, size.toFloat(), size.toFloat())
                    }
                }
                2 -> {
                    for (i in 0 until 2) {
                        val b = bitmaps[i]
                        if (texts[i] != null) {
                            val offset = b.width * .2f
                            val src =
                                Rect(
                                    offset.toInt(),
                                    offset.toInt(),
                                    b.width - offset.toInt(),
                                    b.height - offset.toInt(),
                                )
                            val dst =
                                if (i == 0) {
                                    RectF(0f, 0f, size / 2f, size.toFloat())
                                } else {
                                    RectF(size / 2f, 0f, size.toFloat(), size.toFloat())
                                }
                            canvas.drawBitmap(b, src, dst, null)
                        } else {
                            val m = Matrix()
                            val offset = size / 4f
                            val src =
                                when (i) {
                                    0 -> RectF(offset, 0f, b.width.toFloat() + offset, b.height.toFloat())
                                    else -> RectF(-offset, 0f, b.width.toFloat() - offset, b.height.toFloat())
                                }
                            val dst =
                                when (i) {
                                    0 -> RectF(0f, 0f, size.toFloat(), size.toFloat())
                                    else -> RectF(0f, 0f, size.toFloat(), size.toFloat())
                                }

                            setPaintAndRect(b, m, src, dst, paint, rectF)

                            val angle = if (i == 0) -180f else 180f
                            canvas.drawArc(rectF, -90f, angle, true, paint)
                        }

                        if (texts[i] != null) {
                            textPaint.textSize = textSizeMedium
                            val t = texts[i].toString()
                            canvas.drawAvatarText(t, textPaint, size / 4f + i * size / 2f, size / 2f, size / 2f, size.toFloat())
                        }

                        canvas.drawRect(verticalDividerRectF, verticalDividerPaint)
                    }
                }
                3 -> {
                    for (i in 0 until 3) {
                        val b = bitmaps[i]
                        if (i == 0) {
                            if (texts[i] != null) {
                                val offset = b.width * .2f
                                val src =
                                    Rect(
                                        offset.toInt(),
                                        offset.toInt(),
                                        b.width - offset.toInt(),
                                        b.height - offset.toInt(),
                                    )
                                val dst = RectF(0f, 0f, size / 2f, size.toFloat())
                                canvas.drawBitmap(b, src, dst, null)
                            } else {
                                val m = Matrix()
                                val offset = size / 4f
                                val src = RectF(offset, 0f, b.width.toFloat() + offset, b.height.toFloat())
                                val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
                                setPaintAndRect(b, m, src, dst, paint, rectF)
                                canvas.drawArc(rectF, -90f, -180f, true, paint)
                            }
                        } else {
                            val offset =
                                if (texts[i] != null) {
                                    b.width * .2f
                                } else {
                                    b.width * .1f
                                }
                            val src =
                                Rect(
                                    offset.toInt(),
                                    offset.toInt(),
                                    b.width - offset.toInt(),
                                    b.height - offset.toInt(),
                                )
                            val dst =
                                when (i) {
                                    1 -> {
                                        RectF(size / 2f, 0f, size.toFloat(), size / 2f)
                                    }
                                    else -> {
                                        RectF(size / 2f, size / 2f, size.toFloat(), size.toFloat())
                                    }
                                }
                            canvas.drawBitmap(b, src, dst, null)
                        }

                        if (texts[i] != null) {
                            textPaint.textSize =
                                if (i == 0) {
                                    textSizeMedium
                                } else {
                                    textSizeSmall
                                }
                            val t = texts[i].toString()
                            val centerX = if (i == 0) size / 4f else size * 3f / 4f - textOffset
                            val centerY = when (i) {
                                0 -> size / 2f
                                1 -> size / 4f + textOffset
                                else -> size * 3f / 4f - textOffset
                            }
                            val textWidth = if (i == 0) size / 2f else size / 2f - 2 * textOffset
                            val textHeight = if (i == 0) size.toFloat() else size / 2f - 2 * textOffset
                            canvas.drawAvatarText(t, textPaint, centerX, centerY, textWidth, textHeight)
                        }

                        canvas.drawRect(verticalDividerRectF, verticalDividerPaint)
                        canvas.drawRect(halfHorizontalDividerRectF, halfHorizontalDividerPaint)
                    }
                }
                4 -> {
                    for (i in 0 until 4) {
                        val item = bitmaps[i]
                        val offset =
                            if (texts[i] != null) {
                                item.width * .2f
                            } else {
                                item.width * .1f
                            }
                        val src =
                            Rect(
                                offset.toInt(),
                                offset.toInt(),
                                item.width - offset.toInt(),
                                item.height - offset.toInt(),
                            )
                        val dst =
                            when (i) {
                                0 -> {
                                    RectF(0f, 0f, size / 2f, size / 2f)
                                }
                                1 -> {
                                    RectF(size / 2f, 0f, size.toFloat(), size / 2f)
                                }
                                2 -> {
                                    RectF(0f, size / 2f, size / 2f, size.toFloat())
                                }
                                else -> {
                                    RectF(size / 2f, size / 2f, size.toFloat(), size.toFloat())
                                }
                            }
                        canvas.drawBitmap(item, src, dst, null)

                        if (texts[i] != null) {
                            textPaint.textSize = textSizeSmall
                            val t = texts[i].toString()
                            val centerX = if (i % 2 == 0) size / 4f + textOffset else size * 3f / 4f - textOffset
                            val centerY = if (i < 2) size / 4f + textOffset else size * 3f / 4f - textOffset
                            canvas.drawAvatarText(t, textPaint, centerX, centerY, size / 2f - 2 * textOffset, size / 2f - 2 * textOffset)
                        }

                        canvas.drawRect(verticalDividerRectF, verticalDividerPaint)
                        canvas.drawRect(horizontalDividerRectF, horizontalDividerPaint)
                    }
                }
                else -> {
                }
            }
        }

        private fun getBitmaps(
            bitmaps: MutableList<Bitmap>,
            users: MutableList<User>,
        ) {
            for (i in 0 until users.size) {
                val item = users[i].avatarUrl

                if (item.isNullOrEmpty()) {
                    val user = users[i]
                    texts[i] = AvatarView.checkEmoji(user.fullName)
                    bitmaps.add(getBitmapByPlaceHolder(user.userId))
                } else {
                    bitmaps.add(
                        Glide.with(applicationContext)
                            .asBitmap()
                            .load(item)
                            .submit()
                            .get(10, TimeUnit.SECONDS),
                    )
                }
            }
        }

        private fun setPaintAndRect(
            b: Bitmap,
            m: Matrix,
            src: RectF,
            dst: RectF,
            paint: Paint,
            rectF: RectF,
        ) {
            m.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER)
            val shader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(m)
            paint.shader = shader
            m.mapRect(rectF, src)
        }

        private fun getBitmapByPlaceHolder(userId: String): Bitmap {
            val color =
                try {
                    val num = userId.getColorCode(CodeType.Avatar(avatarArray.size))
                    avatarArray.getOrElse(num) { avatarArray[0] }
                } catch (e: Exception) {
                    -1
                }
            if (color == -1) {
                val d = requireNotNull(ResourcesCompat.getDrawable(applicationContext.resources, R.drawable.default_avatar, applicationContext.theme))
                if (d is BitmapDrawable) {
                    return d.bitmap
                }

                val b = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val c = Canvas(b)
                d.setBounds(0, 0, c.width, c.height)
                d.draw(c)
                return b
            } else {
                val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val c = Canvas(b)
                paint.color = color
                c.drawRect(Rect(0, 0, c.width, c.height), paint)
                return b
            }
        }

        private val paint by lazy {
            Paint().apply {
                style = Paint.Style.FILL
            }
        }

        private val avatarArray by lazy {
            applicationContext.resources.getIntArray(R.array.avatar_colors)
        }
    }

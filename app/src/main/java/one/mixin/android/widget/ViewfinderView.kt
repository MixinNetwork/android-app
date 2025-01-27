package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.widget.ViewfinderView.FrameGravity.values
import one.mixin.android.widget.ViewfinderView.LaserStyle.values
import one.mixin.android.widget.ViewfinderView.TextLocation.values

class ViewfinderView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val DEFAULT_RANGE_RATIO = 1.2f
        private val MAX_ZOOM_RATIO = 1.2f
        private lateinit var paint: Paint
        private lateinit var textPaint: TextPaint
        private var maskColor = 0
        private var frameColor = 0
        private var laserColor = 0
        private var cornerColor = 0
        private var labelTextPadding = 0f
        private var labelTextWidth = 0
        private var labelTextLocation: TextLocation? = null
        private var labelText: String? = null
        private var labelTextColor = 0
        private var labelTextSize = 0f
        private var scannerStart = 0
        private var scannerEnd = 0
        private var frameWidth = 0
        private var frameHeight = 0
        private var laserStyle: LaserStyle? = null
        private var gridColumn = 0
        private var gridHeight = 0
        private lateinit var frame: Rect
        private var cornerRectWidth = 0
        private var cornerRectHeight = 0
        private var scannerLineMoveDistance = 0
        private var scannerLineHeight = 0
        private var frameLineWidth = 0
        private var scannerAnimationDelay = 0
        private var frameRatio = 0f
        private var framePaddingLeft = 0f
        private var framePaddingTop = 0f
        private var framePaddingRight = 0f
        private var framePaddingBottom = 0f
        private var frameGravity: FrameGravity? = null
        private var pointColor = 0
        private var pointStrokeColor = 0
        private var pointBitmap: Bitmap? = null
        private var isShowPointAnim = true
        private var pointRadius = 0f
        private var pointStrokeRatio = 0f
        private var pointStrokeRadius = 0f
        private var currentZoomRatio = 1.0f
        private var lastZoomRatio = 0f
        private val zoomSpeed = 0.02f
        private var zoomCount = 0
        private var pointRangeRadius = 0f
        private var laserBitmap: Bitmap? = null
        private var viewfinderStyle = ViewfinderStyle.CLASSIC
        private var pointList: List<Point>? = null
        var isShowPoints = false
            private set
        private var onItemClickListener: OnItemClickListener? = null
        private var gestureDetector: GestureDetector? = null

        @IntDef(ViewfinderStyle.CLASSIC, ViewfinderStyle.POPULAR)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ViewfinderStyle {
            companion object {
                const val CLASSIC = 0
                const val POPULAR = 1
            }
        }

        enum class LaserStyle(val mValue: Int) {
            NONE(0),
            LINE(1),
            GRID(2),
            RADAR(3),
            IMAGE(4),
            ;

            companion object {
                fun getFromInt(value: Int): LaserStyle {
                    for (style in values()) {
                        if (style.mValue == value) {
                            return style
                        }
                    }
                    return LINE
                }
            }
        }

        enum class TextLocation(private val mValue: Int) {
            TOP(0),
            BOTTOM(1),
            ;

            companion object {
                fun getFromInt(value: Int): TextLocation {
                    for (location in values()) {
                        if (location.mValue == value) {
                            return location
                        }
                    }
                    return TOP
                }
            }
        }

        enum class FrameGravity(val mValue: Int) {
            CENTER(0),
            LEFT(1),
            TOP(2),
            RIGHT(3),
            BOTTOM(4),
            ;

            companion object {
                fun getFromInt(value: Int): FrameGravity {
                    for (gravity in values()) {
                        if (gravity.mValue == value) {
                            return gravity
                        }
                    }
                    return CENTER
                }
            }
        }

        init {
            init(context, attrs)
        }

        private fun init(
            context: Context,
            attrs: AttributeSet?,
        ) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)
            maskColor =
                array.getColor(
                    R.styleable.ViewfinderView_maskColor,
                    ContextCompat.getColor(context, R.color.viewfinder_mask),
                )
            frameColor =
                array.getColor(
                    R.styleable.ViewfinderView_frameColor,
                    ContextCompat.getColor(context, R.color.viewfinder_frame),
                )
            cornerColor =
                array.getColor(
                    R.styleable.ViewfinderView_cornerColor,
                    ContextCompat.getColor(context, R.color.viewfinder_corner),
                )
            laserColor =
                array.getColor(
                    R.styleable.ViewfinderView_laserColor,
                    ContextCompat.getColor(context, R.color.viewfinder_laser),
                )
            labelText = array.getString(R.styleable.ViewfinderView_labelText)
            labelTextColor =
                array.getColor(
                    R.styleable.ViewfinderView_labelTextColor,
                    ContextCompat.getColor(context, R.color.viewfinder_text_color),
                )
            labelTextSize =
                array.getDimension(
                    R.styleable.ViewfinderView_labelTextSize,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics),
                )
            labelTextPadding =
                array.getDimension(
                    R.styleable.ViewfinderView_labelTextPadding,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics),
                )
            labelTextWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_labelTextWidth, 0)
            labelTextLocation =
                TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderView_labelTextLocation, 0))
            frameWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameWidth, 0)
            frameHeight = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameHeight, 0)
            laserStyle =
                LaserStyle.getFromInt(
                    array.getInt(
                        R.styleable.ViewfinderView_laserStyle,
                        LaserStyle.NONE.mValue,
                    ),
                )
            gridColumn = array.getInt(R.styleable.ViewfinderView_gridColumn, 20)
            gridHeight =
                array.getDimension(
                    R.styleable.ViewfinderView_gridHeight,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics),
                ).toInt()
            cornerRectWidth =
                array.getDimension(
                    R.styleable.ViewfinderView_cornerRectWidth,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics),
                ).toInt()
            cornerRectHeight =
                array.getDimension(
                    R.styleable.ViewfinderView_cornerRectHeight,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics),
                ).toInt()
            scannerLineMoveDistance =
                array.getDimension(
                    R.styleable.ViewfinderView_scannerLineMoveDistance,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.5f, resources.displayMetrics),
                ).toInt()
            scannerLineHeight =
                array.getDimension(
                    R.styleable.ViewfinderView_scannerLineHeight,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics),
                ).toInt()
            frameLineWidth =
                array.getDimension(
                    R.styleable.ViewfinderView_frameLineWidth,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics),
                ).toInt()
            scannerAnimationDelay =
                array.getInteger(R.styleable.ViewfinderView_scannerAnimationDelay, 20)
            frameRatio = array.getFloat(R.styleable.ViewfinderView_frameRatio, 0.45f)
            framePaddingLeft = array.getDimension(R.styleable.ViewfinderView_framePaddingLeft, 0f)
            framePaddingTop = array.getDimension(R.styleable.ViewfinderView_framePaddingTop, 0f)
            framePaddingRight = array.getDimension(R.styleable.ViewfinderView_framePaddingRight, 0f)
            framePaddingBottom = array.getDimension(R.styleable.ViewfinderView_framePaddingBottom, 0f)
            frameGravity =
                FrameGravity.getFromInt(
                    array.getInt(
                        R.styleable.ViewfinderView_frameGravity,
                        FrameGravity.CENTER.mValue,
                    ),
                )
            pointColor =
                array.getColor(
                    R.styleable.ViewfinderView_pointColor,
                    ContextCompat.getColor(context, R.color.viewfinder_point),
                )
            pointStrokeColor = array.getColor(R.styleable.ViewfinderView_pointStrokeColor, Color.WHITE)
            pointRadius =
                array.getDimension(
                    R.styleable.ViewfinderView_pointRadius,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics),
                )
            pointStrokeRatio =
                array.getFloat(R.styleable.ViewfinderView_pointStrokeRatio, DEFAULT_RANGE_RATIO)
            isShowPointAnim = array.getBoolean(R.styleable.ViewfinderView_showPointAnim, true)
            val pointDrawable = array.getDrawable(R.styleable.ViewfinderView_pointDrawable)
            val laserDrawable = array.getDrawable(R.styleable.ViewfinderView_laserDrawable)
            viewfinderStyle =
                array.getInt(R.styleable.ViewfinderView_viewfinderStyle, ViewfinderStyle.CLASSIC)
            array.recycle()
            if (pointDrawable != null) {
                pointBitmap = getBitmapFormDrawable(pointDrawable)
                pointRangeRadius =
                    (pointBitmap!!.width + pointBitmap!!.height) / 4 * DEFAULT_RANGE_RATIO
            } else {
                pointStrokeRadius = pointRadius * pointStrokeRatio
                pointRangeRadius = pointStrokeRadius * DEFAULT_RANGE_RATIO
            }
            if (laserDrawable != null) {
                laserBitmap = getBitmapFormDrawable(laserDrawable)
            }
            paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.isAntiAlias = true
            textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            gestureDetector =
                GestureDetector(
                    context,
                    object : SimpleOnGestureListener() {
                        override fun onSingleTapUp(e: MotionEvent): Boolean {
                            return if (isShowPoints && checkSingleTap(e.x, e.y)) {
                                true
                            } else {
                                super.onSingleTapUp(e)
                            }
                        }
                    },
                )
        }

        private fun getBitmapFormDrawable(drawable: Drawable): Bitmap {
            val bitmap =
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565,
                )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(canvas)
            return bitmap
        }

        private fun getBitmapFormDrawable(
            @DrawableRes drawableId: Int,
        ): Bitmap {
            val drawable = requireNotNull(ContextCompat.getDrawable(context, drawableId))
            val bitmap =
                Bitmap.createBitmap(
                    frame.width(),
                    frame.height(),
                    if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565,
                )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, frame.width(), frame.height())
            drawable.draw(canvas)
            return bitmap
        }

        private val displayMetrics: DisplayMetrics
            get() = resources.displayMetrics

        fun setLabelText(labelText: String?) {
            this.labelText = labelText
        }

        fun setLabelTextColor(
            @ColorInt color: Int,
        ) {
            labelTextColor = color
        }

        fun setLabelTextColorResource(
            @ColorRes id: Int,
        ) {
            labelTextColor = ContextCompat.getColor(context, id)
        }

        fun setLabelTextSize(textSize: Float) {
            labelTextSize = textSize
        }

        fun setLaserStyle(laserStyle: LaserStyle?) {
            this.laserStyle = laserStyle
        }

        fun setPointImageResource(
            @DrawableRes drawable: Int,
        ) {
            setPointBitmap(BitmapFactory.decodeResource(resources, drawable))
        }

        private fun setPointBitmap(bitmap: Bitmap?) {
            pointBitmap = bitmap
            pointRangeRadius = (pointBitmap!!.width + pointBitmap!!.height) / 4 * DEFAULT_RANGE_RATIO
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            initFrame(w, h)
        }

        private fun initFrame(
            width: Int,
            height: Int,
        ) {
            val size = (width.coerceAtMost(height) * frameRatio).toInt()
            if (frameWidth <= 0 || frameWidth > width) {
                frameWidth = size
            }
            if (frameHeight <= 0 || frameHeight > height) {
                frameHeight = size
            }
            if (labelTextWidth <= 0) {
                labelTextWidth = width - paddingLeft - paddingRight
            }
            var leftOffsets = (width - frameWidth) / 2 + framePaddingLeft - framePaddingRight
            var topOffsets = (height - frameHeight) / 2 + framePaddingTop - framePaddingBottom
            when (frameGravity) {
                FrameGravity.LEFT -> leftOffsets = framePaddingLeft
                FrameGravity.TOP -> topOffsets = framePaddingTop
                FrameGravity.RIGHT -> leftOffsets = width - frameWidth + framePaddingRight
                FrameGravity.BOTTOM -> topOffsets = height - frameHeight + framePaddingBottom
                else -> {
                    // do noting
                }
            }
            frame =
                Rect(
                    leftOffsets.toInt(),
                    topOffsets.toInt(),
                    leftOffsets.toInt() + frameWidth,
                    topOffsets.toInt() + frameHeight,
                )
            if (laserStyle == LaserStyle.RADAR) {
                this.radarPath.reset()
                this.radarPath.addRoundRect(
                    frame.left.toFloat(),
                    frame.top.toFloat(),
                    frame.right.toFloat(),
                    frame.bottom.toFloat(),
                    cornerRadius,
                    cornerRadius,
                    Path.Direction.CW,
                )
                this.radarPath.close()
            }
        }

        public override fun onDraw(canvas: Canvas) {
            if (isShowPoints) {
                drawMask(canvas, width, height)
                drawResultPoints(canvas, pointList)
                if (isShowPointAnim && pointBitmap == null) {
                    calcZoomPointAnim()
                }
                return
            }
            if (scannerStart == 0 || scannerEnd == 0) {
                scannerStart = frame.top
                scannerEnd = frame.bottom - scannerLineHeight
            }
            when (viewfinderStyle) {
                ViewfinderStyle.CLASSIC -> {
                    drawExterior(canvas, frame, width, height)
                    drawLaserScanner(canvas, frame)
                    drawFrame(canvas, frame)
                    drawCorner(canvas, frame)
                    drawTextInfo(canvas, frame)
                    postInvalidateDelayed(
                        scannerAnimationDelay.toLong(),
                        frame.left,
                        frame.top,
                        frame.right,
                        frame.bottom,
                    )
                }

                ViewfinderStyle.POPULAR -> {
                    drawLaserScanner(canvas, frame)
                    postInvalidateDelayed(scannerAnimationDelay.toLong())
                }
            }
        }

        private fun drawTextInfo(
            canvas: Canvas,
            frame: Rect,
        ) {
            if (!TextUtils.isEmpty(labelText)) {
                textPaint.color = labelTextColor
                textPaint.textSize = labelTextSize
                textPaint.textAlign = Paint.Align.CENTER
                val source = this.labelText ?: ""
                val staticLayout =
                    StaticLayout.Builder.obtain(
                        source,
                        0,
                        source.length,
                        textPaint,
                        labelTextWidth,
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0.0f, 1.2f)
                        .setIncludePad(true)
                        .build()
                if (labelTextLocation == TextLocation.BOTTOM) {
                    canvas.translate(
                        (frame.left + frame.width() / 2).toFloat(),
                        frame.bottom + labelTextPadding,
                    )
                } else {
                    canvas.translate(
                        (frame.left + frame.width() / 2).toFloat(),
                        frame.top - labelTextPadding - staticLayout.height,
                    )
                }
                staticLayout.draw(canvas)
            }
        }

        private fun drawCorner(
            canvas: Canvas,
            frame: Rect,
        ) {
            paint.color = cornerColor
            canvas.drawRect(
                frame.left.toFloat(),
                frame.top.toFloat(),
                (frame.left + cornerRectWidth).toFloat(),
                (frame.top + cornerRectHeight).toFloat(),
                paint,
            )
            canvas.drawRect(
                frame.left.toFloat(),
                frame.top.toFloat(),
                (frame.left + cornerRectHeight).toFloat(),
                (frame.top + cornerRectWidth).toFloat(),
                paint,
            )
            canvas.drawRect(
                (frame.right - cornerRectWidth).toFloat(),
                frame.top.toFloat(),
                frame.right.toFloat(),
                (frame.top + cornerRectHeight).toFloat(),
                paint,
            )
            canvas.drawRect(
                (frame.right - cornerRectHeight).toFloat(),
                frame.top.toFloat(),
                frame.right.toFloat(),
                (frame.top + cornerRectWidth).toFloat(),
                paint,
            )
            canvas.drawRect(
                frame.left.toFloat(),
                (frame.bottom - cornerRectWidth).toFloat(),
                (frame.left + cornerRectHeight).toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
            canvas.drawRect(
                frame.left.toFloat(),
                (frame.bottom - cornerRectHeight).toFloat(),
                (frame.left + cornerRectWidth).toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
            canvas.drawRect(
                (frame.right - cornerRectWidth).toFloat(),
                (frame.bottom - cornerRectHeight).toFloat(),
                frame.right.toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
            canvas.drawRect(
                (frame.right - cornerRectHeight).toFloat(),
                (frame.bottom - cornerRectWidth).toFloat(),
                frame.right.toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
        }

        private fun drawImageScanner(
            canvas: Canvas,
            frame: Rect,
        ) {
            if (laserBitmap != null) {
                paint.color = Color.WHITE
                canvas.drawBitmap(laserBitmap!!, frame.left.toFloat(), scannerStart.toFloat(), paint)
                if (scannerStart < scannerEnd) {
                    scannerStart += scannerLineMoveDistance
                } else {
                    scannerStart = frame.top
                }
            } else {
                drawLineScanner(canvas, frame)
            }
        }

        private val radarPath by lazy {
            Path()
        }

        private val radarPadding = 4.dp.toFloat()
        private val cornerRadius = 32.dp.toFloat()

        private fun drawRadarScanner(
            canvas: Canvas,
            frame: Rect,
        ) {
            val rFrame = frame.toRectF()

            if (scannerStart < scannerEnd) {
                val save = canvas.save()
                canvas.clipPath(radarPath)
                paint.shader = null
                radarGrid.run {
                    canvas.drawBitmap(this, rFrame.left, scannerStart.toFloat() - height, paint)
                }
                paint.shader =
                    LinearGradient(
                        0f,
                        scannerStart + rFrame.height() / 4,
                        0f,
                        scannerStart.toFloat(),
                        Color.TRANSPARENT,
                        Color.argb(153, 170, 255, 224),
                        TileMode.MIRROR,
                    )
                canvas.drawRect(rFrame.left + radarPadding, scannerStart - rFrame.height() / 4, rFrame.right - radarPadding, scannerStart.toFloat(), paint)
                canvas.restoreToCount(save)
                scannerStart += scannerLineMoveDistance
            } else {
                scannerStart = frame.top
            }
            radarFrame.run {
                canvas.drawBitmap(this, rFrame.left, rFrame.top, paint)
            }
        }

        private val radarGrid by lazy {
            getBitmapFormDrawable(R.drawable.scan_grid)
        }

        private val radarFrame by lazy {
            getBitmapFormDrawable(R.drawable.scan_frame)
        }

        private fun drawLaserScanner(
            canvas: Canvas,
            frame: Rect,
        ) {
            if (laserStyle != null) {
                paint.color = laserColor
                when (laserStyle) {
                    LaserStyle.LINE -> drawLineScanner(canvas, frame)
                    LaserStyle.GRID -> drawGridScanner(canvas, frame)
                    LaserStyle.RADAR -> drawRadarScanner(canvas, frame)
                    LaserStyle.IMAGE -> drawImageScanner(canvas, frame)
                    else -> {
                        // do noting
                    }
                }
                paint.shader = null
            }
        }

        private fun drawLineScanner(
            canvas: Canvas,
            frame: Rect,
        ) {
            val linearGradient =
                LinearGradient(
                    frame.left.toFloat(),
                    scannerStart.toFloat(),
                    frame.left.toFloat(),
                    (scannerStart + scannerLineHeight).toFloat(),
                    shadeColor(laserColor),
                    laserColor,
                    TileMode.MIRROR,
                )
            paint.shader = linearGradient
            if (scannerStart < scannerEnd) {
                val rectF =
                    RectF(
                        (frame.left + 2 * scannerLineHeight).toFloat(),
                        scannerStart.toFloat(),
                        (frame.right - 2 * scannerLineHeight).toFloat(),
                        (scannerStart + scannerLineHeight).toFloat(),
                    )
                canvas.drawOval(rectF, paint)
                scannerStart += scannerLineMoveDistance
            } else {
                scannerStart = frame.top
            }
        }

        private fun drawGridScanner(
            canvas: Canvas,
            frame: Rect,
        ) {
            val stroke = 2
            paint.strokeWidth = stroke.toFloat()
            val startY =
                if (gridHeight > 0 && scannerStart - frame.top > gridHeight) scannerStart - gridHeight else frame.top
            val linearGradient =
                LinearGradient(
                    (frame.left + frame.width() / 2).toFloat(),
                    startY.toFloat(),
                    (frame.left + frame.width() / 2).toFloat(),
                    scannerStart.toFloat(),
                    intArrayOf(shadeColor(laserColor), laserColor),
                    floatArrayOf(0f, 1f),
                    TileMode.CLAMP,
                )
            paint.shader = linearGradient
            val wUnit = frame.width() * 1.0f / gridColumn
            for (i in 1 until gridColumn) {
                canvas.drawLine(
                    frame.left + i * wUnit,
                    startY.toFloat(),
                    frame.left + i * wUnit,
                    scannerStart.toFloat(),
                    paint,
                )
            }
            val height =
                if (gridHeight > 0 && scannerStart - frame.top > gridHeight) gridHeight else scannerStart - frame.top
            var i = 0
            while (i <= height / wUnit) {
                canvas.drawLine(
                    frame.left.toFloat(),
                    scannerStart - i * wUnit,
                    frame.right.toFloat(),
                    scannerStart - i * wUnit,
                    paint,
                )
                i++
            }
            if (scannerStart < scannerEnd) {
                scannerStart += scannerLineMoveDistance
            } else {
                scannerStart = frame.top
            }
        }

        fun shadeColor(color: Int): Int {
            val hax = Integer.toHexString(color)
            val result = "01" + hax.substring(2)
            return Integer.valueOf(result, 16)
        }

        private fun drawFrame(
            canvas: Canvas,
            frame: Rect,
        ) {
            paint.color = frameColor
            canvas.drawRect(
                frame.left.toFloat(),
                frame.top.toFloat(),
                frame.right.toFloat(),
                (frame.top + frameLineWidth).toFloat(),
                paint,
            )
            canvas.drawRect(
                frame.left.toFloat(),
                frame.top.toFloat(),
                (frame.left + frameLineWidth).toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
            canvas.drawRect(
                (frame.right - frameLineWidth).toFloat(),
                frame.top.toFloat(),
                frame.right.toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
            canvas.drawRect(
                frame.left.toFloat(),
                (frame.bottom - frameLineWidth).toFloat(),
                frame.right.toFloat(),
                frame.bottom.toFloat(),
                paint,
            )
        }

        private fun drawExterior(
            canvas: Canvas,
            frame: Rect,
            width: Int,
            height: Int,
        ) {
            if (maskColor != 0) {
                paint.color = maskColor
                canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
                canvas.drawRect(
                    0f,
                    frame.top.toFloat(),
                    frame.left.toFloat(),
                    frame.bottom.toFloat(),
                    paint,
                )
                canvas.drawRect(
                    frame.right.toFloat(),
                    frame.top.toFloat(),
                    width.toFloat(),
                    frame.bottom.toFloat(),
                    paint,
                )
                canvas.drawRect(0f, frame.bottom.toFloat(), width.toFloat(), height.toFloat(), paint)
            }
        }

        private fun drawMask(
            canvas: Canvas,
            width: Int,
            height: Int,
        ) {
            if (maskColor != 0) {
                paint.color = maskColor
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }

        private fun drawResultPoints(
            canvas: Canvas,
            points: List<Point>?,
        ) {
            paint.color = Color.WHITE
            if (points != null) {
                for (point in points) {
                    drawResultPoint(canvas, point, currentZoomRatio)
                }
            }
        }

        private fun calcZoomPointAnim() {
            if (currentZoomRatio <= 1f) {
                lastZoomRatio = currentZoomRatio
                currentZoomRatio += zoomSpeed
                if (zoomCount < 2) {
                    zoomCount++
                } else {
                    zoomCount = 0
                }
            } else if (currentZoomRatio >= MAX_ZOOM_RATIO) {
                lastZoomRatio = currentZoomRatio
                currentZoomRatio -= zoomSpeed
            } else {
                if (lastZoomRatio > currentZoomRatio) {
                    lastZoomRatio = currentZoomRatio
                    currentZoomRatio -= zoomSpeed
                } else {
                    lastZoomRatio = currentZoomRatio
                    currentZoomRatio += zoomSpeed
                }
            }
            postInvalidateDelayed((if (zoomCount == 0 && lastZoomRatio == 1f) 3000 else scannerAnimationDelay * 2).toLong())
        }

        private fun drawResultPoint(
            canvas: Canvas,
            point: Point,
            currentZoomRatio: Float,
        ) {
            if (pointBitmap != null) {
                val left = point.x - pointBitmap!!.width / 2.0f
                val top = point.y - pointBitmap!!.height / 2.0f
                canvas.drawBitmap(pointBitmap!!, left, top, paint)
            } else {
                paint.color = pointStrokeColor
                canvas.drawCircle(
                    point.x.toFloat(),
                    point.y.toFloat(),
                    pointStrokeRadius * currentZoomRatio,
                    paint,
                )
                paint.color = pointColor
                canvas.drawCircle(
                    point.x.toFloat(),
                    point.y.toFloat(),
                    pointRadius * currentZoomRatio,
                    paint,
                )
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (isShowPoints) {
                gestureDetector!!.onTouchEvent(event)
            }
            return isShowPoints || super.onTouchEvent(event)
        }

        private fun checkSingleTap(
            x: Float,
            y: Float,
        ): Boolean {
            if (pointList != null) {
                for (i in pointList!!.indices) {
                    val point = pointList!![i]
                    val distance = getDistance(x, y, point.x.toFloat(), point.y.toFloat())
                    if (distance <= pointRangeRadius) {
                        if (onItemClickListener != null) {
                            onItemClickListener!!.onItemClick(i)
                        }
                        return true
                    }
                }
            }
            return true
        }

        private fun getDistance(
            x1: Float,
            y1: Float,
            x2: Float,
            y2: Float,
        ): Float {
            return Math.sqrt(Math.pow((x1 - x2).toDouble(), 2.0) + Math.pow((y1 - y2).toDouble(), 2.0))
                .toFloat()
        }

        fun showScanner() {
            isShowPoints = false
            invalidate()
        }

        fun showResultPoints(points: List<Point>?) {
            pointList = points
            isShowPoints = true
            zoomCount = 0
            lastZoomRatio = 0f
            currentZoomRatio = 1f
            invalidate()
        }

        fun setOnItemClickListener(listener: OnItemClickListener?) {
            onItemClickListener = listener
        }

        interface OnItemClickListener {
            fun onItemClick(position: Int)
        }
    }

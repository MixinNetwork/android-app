package one.mixin.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import one.mixin.android.R;

public class ViewfinderView extends View {


    private final float DEFAULT_RANGE_RATIO = 1.2F;

    private final float MAX_ZOOM_RATIO = 1.2F;


    private Paint paint;


    private TextPaint textPaint;

    private int maskColor;

    private int frameColor;

    private int laserColor;

    private int cornerColor;


    private float labelTextPadding;

    private int labelTextWidth;

    private TextLocation labelTextLocation;

    private String labelText;

    private int labelTextColor;

    private float labelTextSize;


    public int scannerStart = 0;

    public int scannerEnd = 0;


    private int frameWidth;

    private int frameHeight;

    private LaserStyle laserStyle;


    private int gridColumn;

    private int gridHeight;


    private Rect frame;


    private int cornerRectWidth;

    private int cornerRectHeight;

    private int scannerLineMoveDistance;

    private int scannerLineHeight;


    private int frameLineWidth;


    private int scannerAnimationDelay;


    private float frameRatio;


    private float framePaddingLeft;
    private float framePaddingTop;
    private float framePaddingRight;
    private float framePaddingBottom;

    private FrameGravity frameGravity;

    private int pointColor;
    private int pointStrokeColor;
    private Bitmap pointBitmap;
    private boolean isShowPointAnim = true;

    private float pointRadius;
    private float pointStrokeRatio;
    private float pointStrokeRadius;


    private float currentZoomRatio = 1.0f;

    private float lastZoomRatio;

    private float zoomSpeed = 0.02f;

    private int zoomCount;


    private float pointRangeRadius;

    private Bitmap laserBitmap;

    private int viewfinderStyle = ViewfinderStyle.CLASSIC;

    private List<Point> pointList;

    private boolean isShowPoints = false;

    private OnItemClickListener onItemClickListener;

    private GestureDetector gestureDetector;


    @IntDef({ViewfinderStyle.CLASSIC, ViewfinderStyle.POPULAR, ViewfinderStyle.RADAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewfinderStyle {

        int CLASSIC = 0;

        int POPULAR = 1;

        int RADAR = 2;

    }

    public enum LaserStyle {
        NONE(0), LINE(1), GRID(2), IMAGE(3);
        private int mValue;

        LaserStyle(int value) {
            mValue = value;
        }

        private static LaserStyle getFromInt(int value) {
            for (LaserStyle style : LaserStyle.values()) {
                if (style.mValue == value) {
                    return style;
                }
            }
            return LaserStyle.LINE;
        }
    }

    public enum TextLocation {
        TOP(0), BOTTOM(1);

        private int mValue;

        TextLocation(int value) {
            mValue = value;
        }

        private static TextLocation getFromInt(int value) {
            for (TextLocation location : TextLocation.values()) {
                if (location.mValue == value) {
                    return location;
                }
            }
            return TextLocation.TOP;
        }
    }


    public enum FrameGravity {
        CENTER(0), LEFT(1), TOP(2), RIGHT(3), BOTTOM(4);

        private int mValue;

        FrameGravity(int value) {
            mValue = value;
        }

        private static FrameGravity getFromInt(int value) {
            for (FrameGravity gravity : values()) {
                if (gravity.mValue == value) {
                    return gravity;
                }
            }
            return CENTER;
        }
    }

    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView);
        maskColor = array.getColor(R.styleable.ViewfinderView_maskColor, ContextCompat.getColor(context, R.color.viewfinder_mask));
        frameColor = array.getColor(R.styleable.ViewfinderView_frameColor, ContextCompat.getColor(context, R.color.viewfinder_frame));
        cornerColor = array.getColor(R.styleable.ViewfinderView_cornerColor, ContextCompat.getColor(context, R.color.viewfinder_corner));
        laserColor = array.getColor(R.styleable.ViewfinderView_laserColor, ContextCompat.getColor(context, R.color.viewfinder_laser));

        labelText = array.getString(R.styleable.ViewfinderView_labelText);
        labelTextColor = array.getColor(R.styleable.ViewfinderView_labelTextColor, ContextCompat.getColor(context, R.color.viewfinder_text_color));
        labelTextSize = array.getDimension(R.styleable.ViewfinderView_labelTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, getResources().getDisplayMetrics()));
        labelTextPadding = array.getDimension(R.styleable.ViewfinderView_labelTextPadding, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        labelTextWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_labelTextWidth, 0);
        labelTextLocation = TextLocation.getFromInt(array.getInt(R.styleable.ViewfinderView_labelTextLocation, 0));

        frameWidth = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameWidth, 0);
        frameHeight = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameHeight, 0);

        laserStyle = LaserStyle.getFromInt(array.getInt(R.styleable.ViewfinderView_laserStyle, LaserStyle.LINE.mValue));
        gridColumn = array.getInt(R.styleable.ViewfinderView_gridColumn, 20);
        gridHeight = (int) array.getDimension(R.styleable.ViewfinderView_gridHeight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));

        cornerRectWidth = (int) array.getDimension(R.styleable.ViewfinderView_cornerRectWidth, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        cornerRectHeight = (int) array.getDimension(R.styleable.ViewfinderView_cornerRectHeight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
        scannerLineMoveDistance = (int) array.getDimension(R.styleable.ViewfinderView_scannerLineMoveDistance, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
        scannerLineHeight = (int) array.getDimension(R.styleable.ViewfinderView_scannerLineHeight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
        frameLineWidth = (int) array.getDimension(R.styleable.ViewfinderView_frameLineWidth, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        scannerAnimationDelay = array.getInteger(R.styleable.ViewfinderView_scannerAnimationDelay, 20);
        frameRatio = array.getFloat(R.styleable.ViewfinderView_frameRatio, 0.625f);
        framePaddingLeft = array.getDimension(R.styleable.ViewfinderView_framePaddingLeft, 0);
        framePaddingTop = array.getDimension(R.styleable.ViewfinderView_framePaddingTop, 0);
        framePaddingRight = array.getDimension(R.styleable.ViewfinderView_framePaddingRight, 0);
        framePaddingBottom = array.getDimension(R.styleable.ViewfinderView_framePaddingBottom, 0);
        frameGravity = FrameGravity.getFromInt(array.getInt(R.styleable.ViewfinderView_frameGravity, FrameGravity.CENTER.mValue));

        pointColor = array.getColor(R.styleable.ViewfinderView_pointColor, ContextCompat.getColor(context, R.color.viewfinder_point));
        pointStrokeColor = array.getColor(R.styleable.ViewfinderView_pointStrokeColor, Color.WHITE);
        pointRadius = array.getDimension(R.styleable.ViewfinderView_pointRadius, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics()));
        pointStrokeRatio = array.getFloat(R.styleable.ViewfinderView_pointStrokeRatio, DEFAULT_RANGE_RATIO);
        isShowPointAnim = array.getBoolean(R.styleable.ViewfinderView_showPointAnim, true);
        Drawable pointDrawable = array.getDrawable(R.styleable.ViewfinderView_pointDrawable);
        Drawable laserDrawable = array.getDrawable(R.styleable.ViewfinderView_laserDrawable);
        viewfinderStyle = array.getInt(R.styleable.ViewfinderView_viewfinderStyle, ViewfinderStyle.CLASSIC);

        array.recycle();

        if (pointDrawable != null) {
            pointBitmap = getBitmapFormDrawable(pointDrawable);
            pointRangeRadius = (pointBitmap.getWidth() + pointBitmap.getHeight()) / 4 * DEFAULT_RANGE_RATIO;
        } else {
            pointStrokeRadius = pointRadius * pointStrokeRatio;
            pointRangeRadius = pointStrokeRadius * DEFAULT_RANGE_RATIO;
        }

        if (laserDrawable != null) {
            laserBitmap = getBitmapFormDrawable(laserDrawable);
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isShowPoints && checkSingleTap(e.getX(), e.getY())) {
                    return true;
                }
                return super.onSingleTapUp(e);
            }
        });

    }

    private Bitmap getBitmapFormDrawable(@NonNull Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private DisplayMetrics getDisplayMetrics() {
        return getResources().getDisplayMetrics();
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public void setLabelTextColor(@ColorInt int color) {
        this.labelTextColor = color;
    }

    public void setLabelTextColorResource(@ColorRes int id) {
        this.labelTextColor = ContextCompat.getColor(getContext(), id);
    }

    public void setLabelTextSize(float textSize) {
        this.labelTextSize = textSize;
    }

    public void setLaserStyle(LaserStyle laserStyle) {
        this.laserStyle = laserStyle;
    }


    public void setPointImageResource(@DrawableRes int drawable) {
        setPointBitmap(BitmapFactory.decodeResource(getResources(), drawable));
    }

    public void setPointBitmap(Bitmap bitmap) {
        pointBitmap = bitmap;
        pointRangeRadius = (pointBitmap.getWidth() + pointBitmap.getHeight()) / 4 * DEFAULT_RANGE_RATIO;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initFrame(w, h);
    }

    private void initFrame(int width, int height) {

        int size = (int) (Math.min(width, height) * frameRatio);

        if (frameWidth <= 0 || frameWidth > width) {
            frameWidth = size;
        }

        if (frameHeight <= 0 || frameHeight > height) {
            frameHeight = size;
        }

        if (labelTextWidth <= 0) {
            labelTextWidth = width - getPaddingLeft() - getPaddingRight();
        }

        float leftOffsets = (width - frameWidth) / 2 + framePaddingLeft - framePaddingRight;
        float topOffsets = (height - frameHeight) / 2 + framePaddingTop - framePaddingBottom;
        switch (frameGravity) {
            case LEFT:
                leftOffsets = framePaddingLeft;
                break;
            case TOP:
                topOffsets = framePaddingTop;
                break;
            case RIGHT:
                leftOffsets = width - frameWidth + framePaddingRight;
                break;
            case BOTTOM:
                topOffsets = height - frameHeight + framePaddingBottom;
                break;
        }

        frame = new Rect((int) leftOffsets, (int) topOffsets, (int) leftOffsets + frameWidth, (int) topOffsets + frameHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (isShowPoints) {
            drawMask(canvas, canvas.getWidth(), canvas.getHeight());
            drawResultPoints(canvas, pointList);
            if (isShowPointAnim && pointBitmap == null) {
                calcZoomPointAnim();
            }
            return;
        }

        if (frame == null) {
            return;
        }

        if (scannerStart == 0 || scannerEnd == 0) {
            scannerStart = frame.top;
            scannerEnd = frame.bottom - scannerLineHeight;
        }

        if (viewfinderStyle == ViewfinderStyle.CLASSIC) {
            drawExterior(canvas, frame, canvas.getWidth(), canvas.getHeight());
            drawLaserScanner(canvas, frame);
            drawFrame(canvas, frame);
            drawCorner(canvas, frame);
            drawTextInfo(canvas, frame);
            postInvalidateDelayed(scannerAnimationDelay, frame.left, frame.top, frame.right, frame.bottom);
        } else if (viewfinderStyle == ViewfinderStyle.POPULAR) {
            drawLaserScanner(canvas, frame);
            postInvalidateDelayed(scannerAnimationDelay);
        } else if (viewfinderStyle == ViewfinderStyle.RADAR) {
            drawRadar(canvas, frame);
        }

    }

    private void drawRadar(Canvas canvas, Rect frame) {

    }


    private void drawTextInfo(Canvas canvas, Rect frame) {
        if (!TextUtils.isEmpty(labelText)) {
            textPaint.setColor(labelTextColor);
            textPaint.setTextSize(labelTextSize);
            textPaint.setTextAlign(Paint.Align.CENTER);
            StaticLayout staticLayout = new StaticLayout(labelText, textPaint, labelTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, true);
            if (labelTextLocation == TextLocation.BOTTOM) {
                canvas.translate(frame.left + frame.width() / 2, frame.bottom + labelTextPadding);
            } else {
                canvas.translate(frame.left + frame.width() / 2, frame.top - labelTextPadding - staticLayout.getHeight());
            }
            staticLayout.draw(canvas);
        }

    }


    private void drawCorner(Canvas canvas, Rect frame) {
        paint.setColor(cornerColor);

        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectWidth, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + cornerRectHeight, frame.top + cornerRectWidth, paint);

        canvas.drawRect(frame.right - cornerRectWidth, frame.top, frame.right, frame.top + cornerRectHeight, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.top, frame.right, frame.top + cornerRectWidth, paint);

        canvas.drawRect(frame.left, frame.bottom - cornerRectWidth, frame.left + cornerRectHeight, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerRectHeight, frame.left + cornerRectWidth, frame.bottom, paint);

        canvas.drawRect(frame.right - cornerRectWidth, frame.bottom - cornerRectHeight, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerRectHeight, frame.bottom - cornerRectWidth, frame.right, frame.bottom, paint);
    }


    private void drawImageScanner(Canvas canvas, Rect frame) {
        if (laserBitmap != null) {
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(laserBitmap, frame.left, scannerStart, paint);
            if (scannerStart < scannerEnd) {
                scannerStart += scannerLineMoveDistance;
            } else {
                scannerStart = frame.top;
            }
        } else {
            drawLineScanner(canvas, frame);
        }

    }

    private void drawLaserScanner(Canvas canvas, Rect frame) {
        if (laserStyle != null) {
            paint.setColor(laserColor);
            switch (laserStyle) {
                case LINE:
                    drawLineScanner(canvas, frame);
                    break;
                case GRID:
                    drawGridScanner(canvas, frame);
                    break;
                case IMAGE:
                    drawImageScanner(canvas, frame);
                    break;
            }
            paint.setShader(null);
        }
    }

    private void drawLineScanner(Canvas canvas, Rect frame) {
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + scannerLineHeight,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        paint.setShader(linearGradient);
        if (scannerStart < scannerEnd) {
            RectF rectF = new RectF(frame.left + 2 * scannerLineHeight, scannerStart, frame.right - 2 * scannerLineHeight, scannerStart + scannerLineHeight);
            canvas.drawOval(rectF, paint);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }
    }

    private void drawGridScanner(Canvas canvas, Rect frame) {
        int stroke = 2;
        paint.setStrokeWidth(stroke);
        int startY = gridHeight > 0 && scannerStart - frame.top > gridHeight ? scannerStart - gridHeight : frame.top;

        LinearGradient linearGradient = new LinearGradient(frame.left + frame.width() / 2, startY, frame.left + frame.width() / 2, scannerStart, new int[]{shadeColor(laserColor), laserColor}, new float[]{0, 1f}, LinearGradient.TileMode.CLAMP);
        paint.setShader(linearGradient);

        float wUnit = frame.width() * 1.0f / gridColumn;
        float hUnit = wUnit;
        for (int i = 1; i < gridColumn; i++) {
            canvas.drawLine(frame.left + i * wUnit, startY, frame.left + i * wUnit, scannerStart, paint);
        }

        int height = gridHeight > 0 && scannerStart - frame.top > gridHeight ? gridHeight : scannerStart - frame.top;

        for (int i = 0; i <= height / hUnit; i++) {
            canvas.drawLine(frame.left, scannerStart - i * hUnit, frame.right, scannerStart - i * hUnit, paint);
        }

        if (scannerStart < scannerEnd) {
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }

    }

    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "01" + hax.substring(2);
        return Integer.valueOf(result, 16);
    }

    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.top + frameLineWidth, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + frameLineWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - frameLineWidth, frame.top, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - frameLineWidth, frame.right, frame.bottom, paint);
    }

    private void drawExterior(Canvas canvas, Rect frame, int width, int height) {
        if (maskColor != 0) {
            paint.setColor(maskColor);
            canvas.drawRect(0, 0, width, frame.top, paint);
            canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
            canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
            canvas.drawRect(0, frame.bottom, width, height, paint);
        }
    }


    private void drawMask(Canvas canvas, int width, int height) {
        if (maskColor != 0) {
            paint.setColor(maskColor);
            canvas.drawRect(0, 0, width, height, paint);
        }
    }


    private void drawResultPoints(Canvas canvas, List<Point> points) {
        paint.setColor(Color.WHITE);
        if (points != null) {
            for (Point point : points) {
                drawResultPoint(canvas, point, currentZoomRatio);
            }
        }
    }


    private void calcZoomPointAnim() {

        if (currentZoomRatio <= 1f) {
            lastZoomRatio = currentZoomRatio;
            currentZoomRatio += zoomSpeed;

            if (zoomCount < 2) {
                zoomCount++;
            } else {
                zoomCount = 0;
            }
        } else if (currentZoomRatio >= MAX_ZOOM_RATIO) {
            lastZoomRatio = currentZoomRatio;
            currentZoomRatio -= zoomSpeed;
        } else {
            if (lastZoomRatio > currentZoomRatio) {
                lastZoomRatio = currentZoomRatio;
                currentZoomRatio -= zoomSpeed;
            } else {
                lastZoomRatio = currentZoomRatio;
                currentZoomRatio += zoomSpeed;
            }
        }

        postInvalidateDelayed(zoomCount == 0 && lastZoomRatio == 1f ? 3000 : scannerAnimationDelay * 2);

    }


    private void drawResultPoint(Canvas canvas, Point point, float currentZoomRatio) {
        if (pointBitmap != null) {
            float left = point.x - pointBitmap.getWidth() / 2.0f;
            float top = point.y - pointBitmap.getHeight() / 2.0f;
            canvas.drawBitmap(pointBitmap, left, top, paint);
        } else {
            paint.setColor(pointStrokeColor);
            canvas.drawCircle(point.x, point.y, pointStrokeRadius * currentZoomRatio, paint);

            paint.setColor(pointColor);
            canvas.drawCircle(point.x, point.y, pointRadius * currentZoomRatio, paint);
        }


    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isShowPoints) {
            gestureDetector.onTouchEvent(event);
        }
        return isShowPoints || super.onTouchEvent(event);
    }


    private boolean checkSingleTap(float x, float y) {
        if (pointList != null) {
            for (int i = 0; i < pointList.size(); i++) {
                Point point = pointList.get(i);
                float distance = getDistance(x, y, point.x, point.y);
                if (distance <= pointRangeRadius) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(i);
                    }
                    return true;
                }
            }
        }

        return true;
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public boolean isShowPoints() {
        return isShowPoints;
    }


    public void showScanner() {
        isShowPoints = false;
        invalidate();
    }


    public void showResultPoints(List<Point> points) {
        pointList = points;
        isShowPoints = true;
        zoomCount = 0;
        lastZoomRatio = 0;
        currentZoomRatio = 1;
        invalidate();
    }


    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

}
package one.mixin.android.widget.imageeditor.renderers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import one.mixin.android.widget.imageeditor.ColorableRenderer;
import one.mixin.android.widget.imageeditor.RendererContext;

/**
 * Renders a {@link AutomaticControlPointBezierLine} with {@link #thickness}, {@link #color} and {@link #cap} end type.
 */
public final class BezierDrawingRenderer extends InvalidateableRenderer implements ColorableRenderer {

  private final Paint      paint;
  private final AutomaticControlPointBezierLine bezierLine;
  private final Paint.Cap  cap;

  @Nullable
  private final RectF      clipRect;

  private       int        color;
  private       float      thickness;

  private BezierDrawingRenderer(int color, float thickness, @NonNull Paint.Cap cap, @Nullable AutomaticControlPointBezierLine bezierLine, @Nullable RectF clipRect) {
    this.paint      = new Paint();
    this.color      = color;
    this.thickness  = thickness;
    this.cap        = cap;
    this.clipRect   = clipRect;
    this.bezierLine = bezierLine != null ? bezierLine : new AutomaticControlPointBezierLine();

    updatePaint();
  }

  public BezierDrawingRenderer(int color, float thickness, @NonNull Paint.Cap cap, @Nullable RectF clipRect) {
    this(color, thickness, cap,null, clipRect != null ? new RectF(clipRect) : null);
  }

  @Override
  public int getColor() {
    return color;
  }

  @Override
  public void setColor(int color) {
    if (this.color != color) {
      this.color = color;
      updatePaint();
      invalidate();
    }
  }

  public void setThickness(float thickness) {
    if (this.thickness != thickness) {
      this.thickness = thickness;
      updatePaint();
      invalidate();
    }
  }

  private void updatePaint() {
    paint.setColor(color);
    paint.setStrokeWidth(thickness);
    paint.setStyle(Paint.Style.STROKE);
    paint.setAntiAlias(true);
    paint.setStrokeCap(cap);
  }

  public void setFirstPoint(PointF point) {
    bezierLine.reset();
    bezierLine.addPoint(point.x, point.y);
    invalidate();
  }

  public void addNewPoint(PointF point) {
    if (cap != Paint.Cap.ROUND) {
      bezierLine.addPointFiltered(point.x, point.y, thickness * 0.5f);
    } else {
      bezierLine.addPoint(point.x, point.y);
    }
    invalidate();
  }

  @Override
  public void render(@NonNull RendererContext rendererContext) {
    super.render(rendererContext);
    Canvas canvas = rendererContext.canvas;
    canvas.save();
    if (clipRect != null) {
      canvas.clipRect(clipRect);
    }

    int alpha = paint.getAlpha();
    paint.setAlpha(rendererContext.getAlpha(alpha));

    paint.setXfermode(rendererContext.getMaskPaint() != null ? rendererContext.getMaskPaint().getXfermode() : null);

    bezierLine.draw(canvas, paint);

    paint.setAlpha(alpha);
    rendererContext.canvas.restore();
  }

  @Override
  public boolean hitTest(float x, float y) {
    return false;
  }

  public static final Parcelable.Creator<BezierDrawingRenderer> CREATOR = new Parcelable.Creator<BezierDrawingRenderer>() {
    @Override
    public BezierDrawingRenderer createFromParcel(Parcel in) {
      int        color      = in.readInt();
      float      thickness  = in.readFloat();
      Paint.Cap  cap        = Paint.Cap.values()[in.readInt()];
      AutomaticControlPointBezierLine bezierLine = in.readParcelable(AutomaticControlPointBezierLine.class.getClassLoader());
      RectF      clipRect   = in.readParcelable(RectF.class.getClassLoader());

      return new BezierDrawingRenderer(color, thickness, cap, bezierLine, clipRect);
    }

    @Override
    public BezierDrawingRenderer[] newArray(int size) {
      return new BezierDrawingRenderer[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(color);
    dest.writeFloat(thickness);
    dest.writeInt(cap.ordinal());
    dest.writeParcelable(bezierLine, flags);
    dest.writeParcelable(clipRect, flags);
  }

}

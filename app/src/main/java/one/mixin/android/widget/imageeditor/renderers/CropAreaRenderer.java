package one.mixin.android.widget.imageeditor.renderers;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import one.mixin.android.R;
import one.mixin.android.widget.imageeditor.Bounds;
import one.mixin.android.widget.imageeditor.Renderer;
import one.mixin.android.widget.imageeditor.RendererContext;

/**
 * Renders a box outside of the current crop area using {@link R.color#crop_area_renderer_outer_color}
 * and around the edge it renders the markers for the thumbs using {@link R.color#crop_area_renderer_edge_color},
 * {@link R.dimen#crop_area_renderer_edge_thickness} and {@link R.dimen#crop_area_renderer_edge_size}.
 * <p>
 * Hit tests outside of the bounds.
 */
public final class CropAreaRenderer implements Renderer {

  @ColorRes
  private final int     color;
  private final boolean renderCenterThumbs;

  private final Path cropClipPath   = new Path();
  private final Path screenClipPath = new Path();

  private final RectF dst   = new RectF();
  private final Paint paint = new Paint();

  @Override
  public void render(@NonNull RendererContext rendererContext) {
    rendererContext.save();

    Canvas    canvas    = rendererContext.canvas;
    Resources resources = rendererContext.context.getResources();

    canvas.clipPath(cropClipPath);
    canvas.drawColor(ResourcesCompat.getColor(resources, color, null));

    rendererContext.mapRect(dst, Bounds.FULL_BOUNDS);

    final int thickness = resources.getDimensionPixelSize(R.dimen.crop_area_renderer_edge_thickness);
    final int size      = (int) Math.min(resources.getDimensionPixelSize(R.dimen.crop_area_renderer_edge_size), Math.min(dst.width(), dst.height()) / 3f - 10);

    paint.setColor(ResourcesCompat.getColor(resources, R.color.crop_area_renderer_edge_color, null));

    rendererContext.canvasMatrix.setToIdentity();
    screenClipPath.reset();
    screenClipPath.moveTo(dst.left, dst.top);
    screenClipPath.lineTo(dst.right, dst.top);
    screenClipPath.lineTo(dst.right, dst.bottom);
    screenClipPath.lineTo(dst.left, dst.bottom);
    screenClipPath.close();
    canvas.clipPath(screenClipPath);
    canvas.translate(dst.left, dst.top);

    float halfDx = (dst.right - dst.left - size + thickness) / 2;
    float halfDy = (dst.bottom - dst.top - size + thickness) / 2;

    canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(0, halfDy);
    if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(0, halfDy);
    canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(halfDx, 0);
    if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(halfDx, 0);
    canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(0, -halfDy);
    if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(0, -halfDy);
    canvas.drawRect(-thickness, -thickness, size, size, paint);

    canvas.translate(-halfDx, 0);
    if (renderCenterThumbs) canvas.drawRect(-thickness, -thickness, size, size, paint);

    rendererContext.restore();
  }

  public CropAreaRenderer(@ColorRes int color, boolean renderCenterThumbs) {
    this.color              = color;
    this.renderCenterThumbs = renderCenterThumbs;

    cropClipPath.toggleInverseFillType();
    cropClipPath.moveTo(Bounds.LEFT, Bounds.TOP);
    cropClipPath.lineTo(Bounds.RIGHT, Bounds.TOP);
    cropClipPath.lineTo(Bounds.RIGHT, Bounds.BOTTOM);
    cropClipPath.lineTo(Bounds.LEFT, Bounds.BOTTOM);
    cropClipPath.close();
    screenClipPath.toggleInverseFillType();
  }

  @Override
  public boolean hitTest(float x, float y) {
    return !Bounds.contains(x, y);
  }

  public static final Parcelable.Creator<CropAreaRenderer> CREATOR = new Parcelable.Creator<CropAreaRenderer>() {
    @Override
    public @NonNull CropAreaRenderer createFromParcel(@NonNull Parcel in) {
      return new CropAreaRenderer(in.readInt(),
                                  in.readByte() == 1);
    }

    @Override
    public @NonNull CropAreaRenderer[] newArray(int size) {
      return new CropAreaRenderer[size];
    }
  };

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(color);
    dest.writeByte((byte) (renderCenterThumbs ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }
}

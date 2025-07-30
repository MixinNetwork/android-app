package one.mixin.android.widget.imageeditor.renderers;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import one.mixin.android.widget.imageeditor.Renderer;
import one.mixin.android.widget.imageeditor.RendererContext;

/**
 * Maintains a weak reference to the an invalidate callback allowing future invalidation without memory leak risk.
 */
abstract class InvalidateableRenderer implements Renderer {

  private WeakReference<RendererContext.Invalidate> invalidate = new WeakReference<>(null);

  @Override
  public void render(@NonNull RendererContext rendererContext) {
    setInvalidate(rendererContext.invalidate);
  }

  private void setInvalidate(RendererContext.Invalidate invalidate) {
    if (invalidate != this.invalidate.get()) {
      this.invalidate = new WeakReference<>(invalidate);
    }
  }

  protected void invalidate() {
    RendererContext.Invalidate invalidate = this.invalidate.get();
    if (invalidate != null) {
      invalidate.onInvalidate(this);
    }
  }
}

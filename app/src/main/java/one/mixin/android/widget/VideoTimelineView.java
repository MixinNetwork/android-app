package one.mixin.android.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import timber.log.Timber;

public class VideoTimelineView extends View {

    private Long videoLength;
    private final Paint paint;
    private final Paint paint2;
    private boolean pressedPlay;
    private float playProgress = 0f;
    private float pressDx;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private VideoTimelineViewDelegate delegate;
    private final ArrayList<Bitmap> frames = new ArrayList<>();
    private AsyncTask<Integer, Integer, Bitmap> currentTask;
    private static final Object sync = new Object();
    private long frameTimeOffset;
    private int frameWidth;
    private int frameHeight;
    private int framesToLoad;
    private boolean isRoundFrames;
    private Rect rect1;
    private Rect rect2;
    private final RectF rect3 = new RectF();
    private int lastWidth;

    public interface VideoTimelineViewDelegate {

        void onPlayProgressChanged(float progress);

        void didStartDragging();

        void didStopDragging();
    }

    public VideoTimelineView(Context context) {
        this(context, null);
    }

    public VideoTimelineView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xffffffff);
        paint2 = new Paint();
        paint2.setColor(0x7f000000);
    }

    public float getProgress() {
        return playProgress;
    }

    @SuppressWarnings("unused")
    public void setRoundFrames(boolean value) {
        isRoundFrames = value;
        if (isRoundFrames) {
            rect1 = new Rect(dp(14), dp(14), dp(14 + 28), dp(14 + 28));
            rect2 = new Rect();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();

        int width = getMeasuredWidth() - dp(32);
        int playX = (int) (width * playProgress) + dp(16);
        int additionWidthPlay = dp(12);
        if (playX - additionWidthPlay <= x && x <= playX + additionWidthPlay && event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (mediaMetadataRetriever == null) {
                return false;
            }
            if (y >= 0 && y <= getMeasuredHeight()) {
                if (delegate != null) {
                    delegate.didStartDragging();
                }
                pressedPlay = true;
                pressDx = (int) (x - playX);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (pressedPlay) {
                if (delegate != null) {
                    delegate.didStopDragging();
                }
                pressedPlay = false;
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedPlay) {
                playX = (int) (x - pressDx);
                if (playX < dp(16)) {
                    playX = dp(16);
                } else if (playX > width + dp(16)) {
                    playX = width + dp(16);
                }
                playProgress = (float) (playX - dp(16)) / (float) width;
                if (delegate != null) {
                    delegate.onPlayProgressChanged(playProgress);
                }
                invalidate();
                return true;
            }
        }
        return false;
    }

    public void setColor(int color) {
        paint.setColor(color);
    }


    private final long start = 0;

    public void setVideoPath(String path) {
        destroy();
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(path);
        videoLength = Long.valueOf(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        invalidate();
    }

    public void setDelegate(VideoTimelineViewDelegate delegate) {
        this.delegate = delegate;
    }

    @SuppressLint("StaticFieldLeak")
    private void reloadFrames(int frameNum) {
        if (mediaMetadataRetriever == null) {
            return;
        }
        if (frameNum == 0) {
            if (isRoundFrames) {
                frameHeight = frameWidth = dp(36);
                framesToLoad = (int) Math.ceil((getMeasuredWidth() - dp(16)) / (frameHeight / 2.0f));
            } else {
                frameHeight = dp(36);
                framesToLoad = (getMeasuredWidth() - dp(16)) / frameHeight;
                frameWidth = (int) Math.ceil((float) (getMeasuredWidth() - dp(16)) / (float) framesToLoad);
            }
            frameTimeOffset = videoLength / framesToLoad;
        }
        //noinspection deprecation
        currentTask = new AsyncTask<>() {
            private int frameNum = 0;

            @Override
            protected Bitmap doInBackground(Integer... objects) {
                frameNum = objects[0];
                Bitmap bitmap = null;
                if (isCancelled()) {
                    return null;
                }
                try {
                    bitmap = mediaMetadataRetriever.getFrameAtTime((start + frameTimeOffset * frameNum) * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (isCancelled()) {
                        return null;
                    }
                    if (bitmap != null) {
                        Bitmap result = Bitmap.createBitmap(frameWidth, frameHeight, bitmap.getConfig());
                        Canvas canvas = new Canvas(result);
                        float scaleX = (float) frameWidth / (float) bitmap.getWidth();
                        float scaleY = (float) frameHeight / (float) bitmap.getHeight();
                        float scale = Math.max(scaleX, scaleY);
                        int w = (int) (bitmap.getWidth() * scale);
                        int h = (int) (bitmap.getHeight() * scale);
                        Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        Rect destRect = new Rect((frameWidth - w) / 2, (frameHeight - h) / 2, w, h);
                        canvas.drawBitmap(bitmap, srcRect, destRect, null);
                        bitmap.recycle();
                        bitmap = result;
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (!isCancelled()) {
                    frames.add(bitmap);

                    invalidate();
                    if (frameNum < framesToLoad) {
                        reloadFrames(frameNum + 1);
                    }
                }
            }
        };
        currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameNum, null, null);
    }

    public void destroy() {
        synchronized (sync) {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    @SuppressWarnings("unused")
    public boolean isDragging() {
        return pressedPlay;
    }

    public void setProgress(float value) {
        playProgress = value;
        invalidate();
    }

    public void clearFrames() {
        for (Bitmap bitmap : frames) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        frames.clear();
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (lastWidth != widthSize) {
            clearFrames();
            lastWidth = widthSize;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getMeasuredWidth() - dp(36);
        int startX = dp(16);
        int endX = width + dp(16);

        int borderoffset = dp(2);

        int borderTop = dp(16);
        int needleTop = borderTop - borderoffset;
        int borderBottom = borderTop + dp(40);
        int needleBottom = borderBottom + borderoffset;

        canvas.save();
        canvas.clipRect(dp(16), needleTop, width + dp(20), needleBottom);
        if (frames.isEmpty() && currentTask == null) {
            reloadFrames(0);
        } else {
            int offset = 0;
            for (int a = 0; a < frames.size(); a++) {
                Bitmap bitmap = frames.get(a);
                if (bitmap != null) {
                    int x = dp(16) + offset * (isRoundFrames ? frameWidth / 2 : frameWidth);
                    int y = borderTop + borderoffset;
                    if (isRoundFrames) {
                        rect2.set(x, y, x + dp(28), y + dp(28));
                        canvas.drawBitmap(bitmap, rect1, rect2, null);
                    } else {
                        canvas.drawBitmap(bitmap, x, y, null);
                    }
                }
                offset++;
            }
        }

        int top = borderoffset + borderTop;

        canvas.drawRect(dp(16), top, startX, dp(42), paint2);
        canvas.drawRect(endX + dp(4), top, dp(16) + width + dp(4), dp(42), paint2);

        canvas.drawRect(startX, borderTop, startX + borderoffset, borderBottom, paint);
        canvas.drawRect(endX + borderoffset, borderTop, endX + dp(4), borderBottom, paint);
        canvas.drawRect(startX + borderoffset, borderTop, endX + dp(4), top, paint);
        canvas.drawRect(startX + borderoffset, borderBottom - borderoffset, endX + dp(4), borderBottom, paint);
        canvas.restore();

        float cx = dp(18) + width * playProgress;
        rect3.set(cx - dp(1.5f), needleTop, cx + dp(1.5f), needleBottom);
        canvas.drawRoundRect(rect3, dp(1), dp(1), paint2);

        rect3.set(cx - dp(1), needleTop, cx + dp(1), needleBottom);
        canvas.drawRoundRect(rect3, dp(1), dp(1), paint);

    }

    private float density = 0;

    private int dp(float value) {
        if (value == 0) {
            return 0;
        }
        if (density == 0) {
            density = getContext().getResources().getDisplayMetrics().density;
        }
        return (int) Math.ceil(density * value);
    }

}

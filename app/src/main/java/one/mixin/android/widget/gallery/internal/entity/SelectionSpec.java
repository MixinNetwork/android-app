package one.mixin.android.widget.gallery.internal.entity;

import android.content.pm.ActivityInfo;
import android.provider.MediaStore;
import one.mixin.android.widget.gallery.MimeType;
import one.mixin.android.widget.gallery.engine.ImageEngine;
import one.mixin.android.widget.gallery.engine.impl.GlideEngine;
import one.mixin.android.widget.gallery.filter.Filter;
import one.mixin.android.widget.gallery.listener.OnSelectedListener;

import java.util.List;
import java.util.Set;

public final class SelectionSpec {

    public Set<MimeType> mimeTypeSet;
    public boolean mediaTypeExclusive;
    public boolean showSingleMediaType;
    public int orientation;
    public boolean countable;
    public int maxSelectable;
    public int maxImageSelectable;
    public int maxVideoSelectable;
    public List<Filter> filters;
    public boolean capture;
    public CaptureStrategy captureStrategy;
    public int spanCount;
    public int gridExpectedSize;
    public float thumbnailScale;
    public ImageEngine imageEngine;
    public boolean hasInited;
    public OnSelectedListener onSelectedListener;
    public boolean originalable;
    public int originalMaxSize;
    public boolean preview = true;

    private SelectionSpec() {
    }

    public static SelectionSpec getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static SelectionSpec getCleanInstance() {
        SelectionSpec selectionSpec = getInstance();
        selectionSpec.reset();
        return selectionSpec;
    }

    private void reset() {
        mimeTypeSet = null;
        mediaTypeExclusive = true;
        showSingleMediaType = false;
        orientation = 0;
        countable = false;
        maxSelectable = 1;
        maxImageSelectable = 0;
        maxVideoSelectable = 0;
        filters = null;
        capture = false;
        captureStrategy = null;
        spanCount = 3;
        gridExpectedSize = 0;
        thumbnailScale = 0.5f;
        imageEngine = new GlideEngine();
        hasInited = true;
        originalable = false;
        originalMaxSize = Integer.MAX_VALUE;
    }

    public boolean singleSelectionModeEnabled() {
        return !countable && (maxSelectable == 1 || (maxImageSelectable == 1 && maxVideoSelectable == 1));
    }

    public boolean needOrientationRestriction() {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    public boolean onlyShowImages() {
        return showSingleMediaType && (MimeType.ofImage().containsAll(mimeTypeSet) || MimeType.ofSticker().containsAll(mimeTypeSet));
    }

    public boolean onlyShowVideos() {
        return showSingleMediaType && MimeType.ofVideo().containsAll(mimeTypeSet);
    }

    public String getMimeTypeWhere() {
        StringBuilder where = new StringBuilder();
        int i = 0;
        for (MimeType item : mimeTypeSet) {
            if (i == mimeTypeSet.size() - 1) {
                where.append(String.format("%s = '%s' ", MediaStore.Images.Media.MIME_TYPE, item.toString()));
            } else {
                where.append(String.format("%s = '%s' OR ", MediaStore.Images.Media.MIME_TYPE, item.toString()));
            }
            i++;
        }
        return where.toString();
    }

    private static final class InstanceHolder {
        private static final SelectionSpec INSTANCE = new SelectionSpec();
    }
}

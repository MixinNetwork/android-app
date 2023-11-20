package one.mixin.android.widget.gallery.filter;

import android.content.Context;
import one.mixin.android.widget.gallery.MimeType;
import one.mixin.android.widget.gallery.internal.entity.IncapableCause;
import one.mixin.android.widget.gallery.internal.entity.Item;

import java.util.Set;

public abstract class Filter {
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE;
    public static final int K = 1024;

    protected abstract Set<MimeType> constraintTypes();

    public abstract IncapableCause filter(Context context, Item item);

    protected boolean needFiltering(Context context, Item item) {
        for (MimeType type : constraintTypes()) {
            if (type.checkType(context.getContentResolver(), item.getContentUri())) {
                return true;
            }
        }
        return false;
    }
}

package one.mixin.android.widget.gallery.internal.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;
import one.mixin.android.R;
import one.mixin.android.widget.gallery.MimeType;
import one.mixin.android.widget.gallery.filter.Filter;
import one.mixin.android.widget.gallery.internal.entity.IncapableCause;
import one.mixin.android.widget.gallery.internal.entity.Item;
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec;
import timber.log.Timber;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public final class PhotoMetadataUtils {
    private static final String TAG = PhotoMetadataUtils.class.getSimpleName();
    private static final int MAX_WIDTH = 1600;
    private static final String SCHEME_CONTENT = "content";

    private PhotoMetadataUtils() {
        throw new AssertionError("oops! the utility class is about to be instantiated...");
    }

    public static int getPixelsCount(ContentResolver resolver, Uri uri) {
        Point size = getBitmapBound(resolver, uri);
        return size.x * size.y;
    }

    public static Point getBitmapSize(Uri uri, Activity activity) {
        ContentResolver resolver = activity.getContentResolver();
        Point imageSize = getBitmapBound(resolver, uri);
        int w = imageSize.x;
        int h = imageSize.y;
        if (PhotoMetadataUtils.shouldRotate(resolver, uri)) {
            w = imageSize.y;
            h = imageSize.x;
        }
        if (h == 0) return new Point(MAX_WIDTH, MAX_WIDTH);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenWidth = (float) metrics.widthPixels;
        float screenHeight = (float) metrics.heightPixels;
        float widthScale = screenWidth / w;
        float heightScale = screenHeight / h;
        if (widthScale > heightScale) {
            return new Point((int) (w * widthScale), (int) (h * heightScale));
        }
        return new Point((int) (w * widthScale), (int) (h * heightScale));
    }

    public static Point getBitmapBound(ContentResolver resolver, Uri uri) {
        InputStream is = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            is = resolver.openInputStream(uri);
            BitmapFactory.decodeStream(is, null, options);
            int width = options.outWidth;
            int height = options.outHeight;
            return new Point(width, height);
        } catch (FileNotFoundException e) {
            return new Point(0, 0);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getPath(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return null;
        }

        if (SCHEME_CONTENT.equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, new String[]{MediaStore.Images.ImageColumns.DATA},
                        null, null, null);
                if (cursor == null || !cursor.moveToFirst()) {
                    return null;
                }
                int indexData = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                return cursor.getString(indexData);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return uri.getPath();
    }

    public static IncapableCause isAcceptable(Context context, Item item) {
        if (!isSelectableType(context, item)) {
            return new IncapableCause(context.getString(R.string.Format_not_supported));
        }

        if (SelectionSpec.getInstance().filters != null) {
            for (Filter filter : SelectionSpec.getInstance().filters) {
                IncapableCause incapableCause = filter.filter(context, item);
                if (incapableCause != null) {
                    return incapableCause;
                }
            }
        }
        return null;
    }

    private static boolean isSelectableType(Context context, Item item) {
        if (context == null) {
            return false;
        }

        ContentResolver resolver = context.getContentResolver();
        for (MimeType type : SelectionSpec.getInstance().mimeTypeSet) {
            if (type.checkType(resolver, item.getContentUri())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    private static boolean shouldRotate(ContentResolver resolver, Uri uri) {
        ExifInterface exif;
        try {
            exif = ExifInterfaceCompat.newInstance(getPath(resolver, uri));
        } catch (IOException e) {
            Timber.e("could not read exif info of the image: %s", uri);
            return false;
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        return orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270;
    }

    public static float getSizeInMB(long sizeInBytes) {
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        df.applyPattern("0.0");
        String result = df.format((float) sizeInBytes / 1024 / 1024);
        Timber.e("getSizeInMB: %s", result);
        result = result.replaceAll(",", "."); // in some case , 0.0 will be 0,0
        return Float.valueOf(result);
    }
}

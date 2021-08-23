package one.mixin.android.widget.gallery.internal.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.loader.content.CursorLoader;

import java.util.Arrays;

import one.mixin.android.widget.gallery.internal.entity.Album;
import one.mixin.android.widget.gallery.internal.entity.Item;
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec;
import one.mixin.android.widget.gallery.internal.utils.MediaStoreCompat;
import timber.log.Timber;

public class AlbumMediaLoader extends CursorLoader {
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            "duration"};

    private static final String SELECTION_ALL =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND (" + MediaStore.Images.Media.MIME_TYPE + "='image/jpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/png'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/gif'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heic'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heif'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heifs'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mp4'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/quicktime'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp2'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/x-matroska'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/webm'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='ideo/mp2ts'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/avi')";

    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };


    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }

    private static final String SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND ( %s )";

    private static final String SELECTION_ALBUM =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND (" + MediaStore.Images.Media.MIME_TYPE + "='image/jpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/png'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/gif'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heic'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heif'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/heifs'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mp4'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/quicktime'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp2'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/x-matroska'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/webm'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='ideo/mp2ts'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/avi')";


    private static final String SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND ( %s )";

    private static String[] getSelectionAlbumArgs(String albumId) {
        return new String[]{
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                albumId
        };
    }

    private static String[] getSelectionAlbumArgsForSingleMediaType(int mediaType, String albumId) {
        return new String[]{String.valueOf(mediaType), albumId};
    }

    private static final String ORDER_BY = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
    private final boolean mEnableCapture;

    private AlbumMediaLoader(Context context, String selection, String[] selectionArgs, boolean capture) {
        super(context, QUERY_URI, PROJECTION, selection, selectionArgs, ORDER_BY);
        mEnableCapture = capture;
    }

    public static CursorLoader newInstance(Context context, Album album, boolean capture) {
        String selection;
        String[] selectionArgs;
        boolean enableCapture;

        if (album.isAll()) {
            if (SelectionSpec.getInstance().onlyShowImages()) {
                selection = String.format(SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
                selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
            } else if (SelectionSpec.getInstance().onlyShowVideos()) {
                selection = String.format(SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
                selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            } else {
                selection = SELECTION_ALL;
                selectionArgs = SELECTION_ALL_ARGS;
            }
            enableCapture = capture;
        } else {
            if (SelectionSpec.getInstance().onlyShowImages()) {
                selection = String.format(SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
                selectionArgs = getSelectionAlbumArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                        album.getId());
            } else if (SelectionSpec.getInstance().onlyShowVideos()) {
                selection = String.format(SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
                selectionArgs = getSelectionAlbumArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        album.getId());
            } else {
                selection = SELECTION_ALBUM;
                selectionArgs = getSelectionAlbumArgs(album.getId());
            }
            enableCapture = false;
        }
        Timber.e("@@@ selection: %s", selection);
        Timber.e("@@@ selection: %s", Arrays.toString(selectionArgs));
        return new AlbumMediaLoader(context, selection, selectionArgs, enableCapture);
    }

    @Override
    public Cursor loadInBackground() {
        Cursor result = super.loadInBackground();
        if (!mEnableCapture || !MediaStoreCompat.hasCameraFeature(getContext())) {
            return result;
        }
        MatrixCursor dummy = new MatrixCursor(PROJECTION);
        dummy.addRow(new Object[]{Item.ITEM_ID_CAPTURE, Item.ITEM_DISPLAY_NAME_CAPTURE, "", 0, 0});
        return new MergeCursor(new Cursor[]{dummy, result});
    }

    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}

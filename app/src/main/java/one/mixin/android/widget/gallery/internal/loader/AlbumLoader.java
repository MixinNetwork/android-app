package one.mixin.android.widget.gallery.internal.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.loader.content.CursorLoader;
import one.mixin.android.widget.gallery.internal.entity.Album;
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec;

public class AlbumLoader extends CursorLoader {
    public static final String COLUMN_COUNT = "count";
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] COLUMNS = {
            MediaStore.Files.FileColumns._ID,
            "bucket_id",
            "bucket_display_name",
            MediaStore.MediaColumns.DATA,
            COLUMN_COUNT};
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            "bucket_id",
            "bucket_display_name",
            MediaStore.MediaColumns.DATA,
            "COUNT(*) AS " + COLUMN_COUNT};

    private static final String SELECTION =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") AND (" + MediaStore.Images.Media.MIME_TYPE + "='image/jpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/png'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/gif'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='image/jpg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mpeg'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/mp4'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/quicktime'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/3gpp2'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/x-matroska'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/webm'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='ideo/mp2ts'"
                    + " OR " + MediaStore.Images.Media.MIME_TYPE + "='video/avi')"
                    + " GROUP BY (bucket_id";

    private static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };

    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") AND ( %s )"
                    + " GROUP BY (bucket_id";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }

    private static final String BUCKET_ORDER_BY = "datetaken DESC";

    private AlbumLoader(Context context, String selection, String[] selectionArgs) {
        super(context, QUERY_URI, PROJECTION, selection, selectionArgs, BUCKET_ORDER_BY);
    }

    public static CursorLoader newInstance(Context context) {
        String selection;
        String[] selectionArgs;
        if (SelectionSpec.getInstance().onlyShowImages()) {
            selection = String.format(SELECTION_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (SelectionSpec.getInstance().onlyShowVideos()) {
            selection = String.format(SELECTION_FOR_SINGLE_MEDIA_TYPE, SelectionSpec.getInstance().getMimeTypeWhere());
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        } else {
            selection = SELECTION;
            selectionArgs = SELECTION_ARGS;
        }
        return new AlbumLoader(context, selection, selectionArgs);
    }

    @Override
    public Cursor loadInBackground() {
        Cursor albums = super.loadInBackground();
        MatrixCursor allAlbum = new MatrixCursor(COLUMNS);
        int totalCount = 0;
        String allAlbumCoverPath = "";
        if (albums != null) {
            while (albums.moveToNext()) {
                totalCount += albums.getInt(albums.getColumnIndex(COLUMN_COUNT));
            }
            if (albums.moveToFirst()) {
                allAlbumCoverPath = albums.getString(albums.getColumnIndex(MediaStore.MediaColumns.DATA));
            }
        }
        allAlbum.addRow(new String[]{Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, allAlbumCoverPath,
                String.valueOf(totalCount)});

        return new MergeCursor(new Cursor[]{allAlbum, albums});
    }

    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}
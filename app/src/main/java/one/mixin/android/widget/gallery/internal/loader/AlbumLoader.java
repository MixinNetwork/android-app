package one.mixin.android.widget.gallery.internal.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.SparseArray;

import androidx.loader.content.CursorLoader;
import one.mixin.android.widget.gallery.internal.entity.Album;
import one.mixin.android.widget.gallery.internal.entity.SelectionSpec;


/**
 * Load all albums (grouped by bucket_id) into a single cursor.
 */
public class AlbumLoader extends CursorLoader {
    private static final String COLUMN_BUCKET_ID = "bucket_id";
    private static final String COLUMN_BUCKET_DISPLAY_NAME = "bucket_display_name";
    public static final String COLUMN_COUNT = "count";
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] COLUMNS = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            COLUMN_COUNT};
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            "COUNT(*) AS " + COLUMN_COUNT};

    private static final String[] PROJECTION_29 = {
            MediaStore.Files.FileColumns._ID,
            COLUMN_BUCKET_ID,
            COLUMN_BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE};

    // === params for showSingleMediaType: false ===
    private static final String SELECTION =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_29 =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE_29 =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.MIME_TYPE + "=?"
                    + ") GROUP BY (bucket_id";
    private static final String SELECTION_FOR_SINGLE_MEDIA_GIF_TYPE_29 =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                    + " AND " + MediaStore.MediaColumns.MIME_TYPE + "=?";

    private static String[] getSelectionArgsForSingleMediaGifType(int mediaType) {
        return new String[]{String.valueOf(mediaType), "image/gif"};
    }
    // =============================================

    private static final String BUCKET_ORDER_BY = "datetaken DESC";

    // workaround with device with too much albums
    private static final int MAX_ALBUM_COUNT = 5000;

    private AlbumLoader(Context context, String selection, String[] selectionArgs) {
        super(
                context,
                QUERY_URI,
                android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? PROJECTION : PROJECTION_29,
                selection,
                selectionArgs,
                BUCKET_ORDER_BY
        );
    }

    public static CursorLoader newInstance(Context context) {
        String selection;
        String[] selectionArgs;
        if (SelectionSpec.getInstance().onlyShowImages()) {
            selection = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? SELECTION_FOR_SINGLE_MEDIA_TYPE : SELECTION_FOR_SINGLE_MEDIA_TYPE_29;
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (SelectionSpec.getInstance().onlyShowVideos()) {
            selection = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? SELECTION_FOR_SINGLE_MEDIA_TYPE : SELECTION_FOR_SINGLE_MEDIA_TYPE_29;
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        } else {
            selection = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? SELECTION : SELECTION_29;
            selectionArgs = SELECTION_ARGS;
        }
        return new AlbumLoader(context, selection, selectionArgs);
    }

    @Override
    public Cursor loadInBackground() {
        Cursor albums = super.loadInBackground();
        MatrixCursor allAlbum = new MatrixCursor(COLUMNS);

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            int totalCount = 0;
            String allAlbumCoverPath = "";
            if (albums != null) {
                int indexCount = albums.getColumnIndex(COLUMN_COUNT);
                int indexData = albums.getColumnIndex(MediaStore.MediaColumns.DATA);
                while (albums.moveToNext()) {
                    totalCount += albums.getInt(indexCount);
                }
                if (albums.moveToFirst()) {
                    allAlbumCoverPath = albums.getString(indexData);
                }
            }

            allAlbum.addRow(new String[]{Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, allAlbumCoverPath, "",
                    String.valueOf(totalCount)});

            return new MergeCursor(new Cursor[]{allAlbum, albums});
        } else {
            int totalCount = 0;
            String allAlbumCoverPath = "";
            SparseArray<Album> albumList = new SparseArray<>();
            if (albums != null) {
                while (albums.moveToNext() && totalCount < MAX_ALBUM_COUNT) {
                    int indexBucket = albums.getColumnIndex(COLUMN_BUCKET_ID);
                    int indexData = albums.getColumnIndex(MediaStore.MediaColumns.DATA);
                    int indexName = albums.getColumnIndex(COLUMN_BUCKET_DISPLAY_NAME);
                    String albumCoverPath = albums.getString(indexData);
                    if ("".equals(allAlbumCoverPath)) {
                        allAlbumCoverPath = albumCoverPath;
                    }
                    int bucketId = albums.getInt(indexBucket);
                    String bucketDisplayName = albums.getString(indexName);
                    Album album = albumList.get(bucketId);
                    if (album == null) {
                        album = new Album(String.valueOf(bucketId), albumCoverPath, bucketDisplayName, 0);
                        albumList.append(bucketId, album);
                    }
                    album.addCaptureCount();
                    totalCount++;
                }
            }

            allAlbum.addRow(new String[]{Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, allAlbumCoverPath, null,
                    String.valueOf(totalCount)});

            for (int i = 0, size = albumList.size(); i < size; i++) {
                Album album = albumList.valueAt(i);
                allAlbum.addRow(new String[]{album.getId(), album.getId(), album.getDisplayName(null), album.getCoverPath(), null,
                        String.valueOf(album.getCount())});
            }

            MergeCursor mergeCursor = new MergeCursor(new Cursor[]{allAlbum});

            return mergeCursor;
        }
    }

    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}
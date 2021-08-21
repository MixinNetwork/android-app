package one.mixin.android.widget.gallery.internal.model;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import java.lang.ref.WeakReference;
import one.mixin.android.widget.gallery.internal.loader.AlbumLoader;
import timber.log.Timber;

public class AlbumCollection implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "AlumCollection";
    private static final int LOADER_ID = 1;
    private static final String STATE_CURRENT_SELECTION = "state_current_selection";
    private WeakReference<Context> mContext;
    private LoaderManager mLoaderManager;
    private AlbumCallbacks mCallbacks;
    private int mCurrentSelection;

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.e("%s onCreateLoader", TAG);
        Context context = mContext.get();
        if (context == null) {
            return null;
        }
        return AlbumLoader.newInstance(context);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        Context context = mContext.get();
        Timber.e("%s onLoadFinished data cursor: %s, context: %s", TAG, data, context);
        if (context == null) {
            return;
        }

        mCallbacks.onAlbumLoad(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        Timber.e("%s onLoaderReset", TAG);
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        mCallbacks.onAlbumReset();
    }

    public void onCreate(FragmentActivity activity, AlbumCallbacks callbacks) {
        Timber.e("%s onCreate", TAG);
        mContext = new WeakReference<>(activity);
        mLoaderManager = LoaderManager.getInstance(activity);
        mCallbacks = callbacks;
    }

    public void onCreate(Fragment fragment, AlbumCallbacks callbacks) {
        Timber.e("%s onCreate", TAG);
        mContext = new WeakReference<>(fragment.requireContext());
        mLoaderManager = LoaderManager.getInstance(fragment);
        mCallbacks = callbacks;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Timber.e("%s onRestoreInstanceState", TAG);
        if (savedInstanceState == null) {
            return;
        }

        mCurrentSelection = savedInstanceState.getInt(STATE_CURRENT_SELECTION);
    }

    public void onSaveInstanceState(Bundle outState) {
        Timber.e("%s onSaveInstanceState", TAG);
        outState.putInt(STATE_CURRENT_SELECTION, mCurrentSelection);
    }

    public void onDestroy() {
        Timber.e("%s onDestroy", TAG);
        if (mLoaderManager != null) {
            mLoaderManager.destroyLoader(LOADER_ID);
        }
        mCallbacks = null;
    }

    public void restartLoader() {
        Timber.e("%s restartLoader", TAG);
        mLoaderManager.restartLoader(LOADER_ID, null, this);
    }

    public void loadAlbums() {
        Timber.e("%s loadAlbums", TAG);
        mLoaderManager.initLoader(LOADER_ID, null, this);
    }

    public int getCurrentSelection() {
        Timber.e("%s getCurrentSelection", TAG);
        return mCurrentSelection;
    }

    public void setStateCurrentSelection(int currentSelection) {
        mCurrentSelection = currentSelection;
    }

    public interface AlbumCallbacks {
        void onAlbumLoad(Cursor cursor);

        void onAlbumReset();
    }
}

package one.mixin.android.widget.media;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import one.mixin.android.R;


public class RecentPhotoRecyclerView extends FrameLayout implements LoaderManager.LoaderCallbacks<Cursor> {

    @NonNull
    private final RecyclerView recyclerView;
    @Nullable
    private OnItemClickedListener listener;

    public RecentPhotoRecyclerView(Context context) {
        this(context, null);
    }

    public RecentPhotoRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentPhotoRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.view_recent_photo, this);

        this.recyclerView = findViewById(R.id.photo_list);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        this.recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    public void setListener(@Nullable OnItemClickedListener listener) {
        this.listener = listener;

        if (this.recyclerView.getAdapter() != null) {
            ((RecentPhotoAdapter) this.recyclerView.getAdapter()).setListener(listener);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new RecentPhotosLoader(getContext());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        RecentPhotoAdapter adapter = new RecentPhotoAdapter(getContext(), data, RecentPhotosLoader.Companion.getBASE_URL(), listener);
        this.recyclerView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorRecyclerViewAdapter) this.recyclerView.getAdapter()).changeCursor(null);
    }

    private static class RecentPhotoAdapter extends CursorRecyclerViewAdapter<RecentPhotoAdapter.RecentPhotoViewHolder> {

        @NonNull
        private final Uri baseUri;
        @Nullable
        private OnItemClickedListener clickedListener;

        private RecentPhotoAdapter(@NonNull Context context, @NonNull Cursor cursor, @NonNull Uri baseUri, @Nullable OnItemClickedListener listener) {
            super(context, cursor);
            this.baseUri = baseUri;
            this.clickedListener = listener;
        }

        @Override
        public RecentPhotoViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recent_photo_view_item, parent, false);

            return new RecentPhotoViewHolder(itemView);
        }

        @SuppressWarnings("unused")
        @Override
        public void onBindItemViewHolder(RecentPhotoViewHolder viewHolder, @NonNull Cursor cursor) {

            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
            long dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN));
            long dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_MODIFIED));
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.MIME_TYPE));
            int orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));

            final Uri uri = Uri.withAppendedPath(baseUri, Long.toString(id));

            Glide.with(getContext()).load(uri).apply(new RequestOptions().centerCrop()).into(viewHolder.imageView);

            viewHolder.imageView.setOnClickListener(v -> {
                if (clickedListener != null) clickedListener.onItemClicked(uri);
            });

        }

        public void setListener(@Nullable OnItemClickedListener listener) {
            this.clickedListener = listener;
        }

        static class RecentPhotoViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;

            RecentPhotoViewHolder(View itemView) {
                super(itemView);
                this.imageView = itemView.findViewById(R.id.thumbnail);
            }
        }
    }

    public interface OnItemClickedListener {
        void onItemClicked(Uri uri);
    }
}
